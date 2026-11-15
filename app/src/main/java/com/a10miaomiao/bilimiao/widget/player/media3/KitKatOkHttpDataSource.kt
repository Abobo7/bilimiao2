package com.a10miaomiao.bilimiao.widget.player.media3

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpUtil
import androidx.media3.datasource.TransferListener
import com.a10miaomiao.bilimiao.comm.network.KitKatTlsHelper
import okhttp3.CacheControl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * 基于 OkHttp 3.12 + Conscrypt (TLS 1.2/1.3) 的 Media3 网络数据源，
 * 解决 Android 4.4 系统 HttpURLConnection 在 B 站 CDN / MCDN 重定向与现代 TLS 握手上的兼容问题。
 */
@OptIn(UnstableApi::class)
class KitKatOkHttpDataSource(
    private val client: OkHttpClient,
    private val userAgent: String?,
    private val defaultRequestProperties: Map<String, String>,
) : BaseDataSource(/* isNetwork = */ true), DataSource {

    class Factory(
        private var userAgent: String? = null,
        private var connectTimeoutMs: Int = 15_000,
        private var readTimeoutMs: Int = 20_000,
        private var allowCrossProtocolRedirects: Boolean = true,
    ) : DataSource.Factory {

        private val defaultRequestProperties = HashMap<String, String>()
        private var transferListener: TransferListener? = null

        fun setUserAgent(ua: String?): Factory {
            this.userAgent = ua
            return this
        }

        fun setDefaultRequestProperties(headers: Map<String, String>?): Factory {
            defaultRequestProperties.clear()
            if (headers != null) {
                defaultRequestProperties.putAll(headers)
            }
            return this
        }

        fun setTransferListener(listener: TransferListener?): Factory {
            this.transferListener = listener
            return this
        }

        override fun createDataSource(): DataSource {
            val okClient = streamingClient.newBuilder()
                .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(allowCrossProtocolRedirects)
                .build()
            val ds = KitKatOkHttpDataSource(
                okClient,
                userAgent,
                HashMap(defaultRequestProperties),
            )
            transferListener?.let { ds.addTransferListener(it) }
            return ds
        }

        companion object {
            private val streamingClient: OkHttpClient by lazy {
                KitKatTlsHelper.createOkHttpClientBuilder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .build()
            }
        }
    }

    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var responseByteStream: InputStream? = null
    private var opened = false
    private var bytesToRead: Long = 0
    private var bytesRead: Long = 0

    override fun getUri(): Uri? {
        return response?.request()?.url()?.toString()?.let { Uri.parse(it) } ?: dataSpec?.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return response?.headers()?.toMultimap() ?: emptyMap()
    }

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.bytesRead = 0
        this.bytesToRead = 0
        transferInitializing(dataSpec)

        val request = buildRequest(dataSpec)
        val resp = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }
        this.response = resp
        val body = resp.body()
        val stream = body?.byteStream()

        val responseCode = resp.code()
        if (!resp.isSuccessful || stream == null) {
            if (responseCode == 416) {
                val documentSize = HttpUtil.getDocumentSize(resp.headers().get("Content-Range"))
                if (dataSpec.position == documentSize) {
                    opened = true
                    transferStarted(dataSpec)
                    return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else 0L
                }
            }
            val errorBody = try {
                body?.bytes() ?: ByteArray(0)
            } catch (_: IOException) {
                ByteArray(0)
            }
            val headers = resp.headers().toMultimap()
            closeConnectionQuietly()
            val cause = if (responseCode == 416) {
                DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            } else {
                null
            }
            throw HttpDataSource.InvalidResponseCodeException(
                responseCode,
                resp.message(),
                cause,
                headers,
                dataSpec,
                errorBody
            )
        }

        this.responseByteStream = stream
        val bytesToSkip = if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0L

        bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val contentLength = body.contentLength()
            if (contentLength != -1L) contentLength - bytesToSkip else C.LENGTH_UNSET.toLong()
        }

        opened = true
        transferStarted(dataSpec)

        try {
            skipFully(bytesToSkip, dataSpec)
        } catch (e: HttpDataSource.HttpDataSourceException) {
            closeConnectionQuietly()
            throw e
        }

        return bytesToRead
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining == 0L) {
                return C.RESULT_END_OF_INPUT
            }
        }
        val readLength = if (bytesToRead == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            min(length.toLong(), bytesToRead - bytesRead).toInt()
        }

        val stream = responseByteStream ?: return C.RESULT_END_OF_INPUT
        val read = try {
            stream.read(buffer, offset, readLength)
        } catch (e: IOException) {
            val spec = dataSpec ?: DataSpec(Uri.EMPTY)
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                spec,
                HttpDataSource.HttpDataSourceException.TYPE_READ
            )
        }

        if (read == -1) {
            return C.RESULT_END_OF_INPUT
        }

        bytesRead += read
        bytesTransferred(read)
        return read
    }

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
            closeConnectionQuietly()
        }
    }

    private fun buildRequest(dataSpec: DataSpec): Request {
        val url = dataSpec.uri.toString()
        val builder = Request.Builder().url(url)
        builder.cacheControl(CacheControl.FORCE_NETWORK)

        val headers = HashMap<String, String>()
        headers.putAll(defaultRequestProperties)
        headers.putAll(dataSpec.httpRequestHeaders)
        for ((k, v) in headers) {
            builder.header(k, v)
        }

        val rangeHeader = HttpUtil.buildRangeRequestHeader(dataSpec.position, dataSpec.length)
        if (rangeHeader != null) {
            builder.addHeader("Range", rangeHeader)
        }
        if (!userAgent.isNullOrEmpty() && !headers.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
            builder.addHeader("User-Agent", userAgent)
        }
        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
            builder.addHeader("Accept-Encoding", "identity")
        }

        val bodyBytes = dataSpec.httpBody
        val requestBody = when {
            bodyBytes != null -> RequestBody.create(null as MediaType?, bodyBytes)
            dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST -> RequestBody.create(null as MediaType?, ByteArray(0))
            else -> null
        }
        builder.method(dataSpec.httpMethodString, requestBody)
        return builder.build()
    }

    private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
        if (bytesToSkip == 0L) return
        val skipBuffer = ByteArray(4096)
        var remaining = bytesToSkip
        val stream = responseByteStream ?: return
        while (remaining > 0) {
            try {
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedIOException()
                }
                val readLength = min(remaining, skipBuffer.size.toLong()).toInt()
                val read = stream.read(skipBuffer, 0, readLength)
                if (read == -1) {
                    throw HttpDataSource.HttpDataSourceException(
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSource.HttpDataSourceException.TYPE_OPEN
                    )
                }
                remaining -= read
                bytesTransferred(read)
            } catch (e: IOException) {
                if (e is HttpDataSource.HttpDataSourceException) throw e
                throw HttpDataSource.HttpDataSourceException.createForIOException(
                    e,
                    dataSpec,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN
                )
            }
        }
    }

    private fun closeConnectionQuietly() {
        try {
            response?.body()?.close()
        } catch (_: Throwable) {
        }
        response = null
        responseByteStream = null
    }
}
