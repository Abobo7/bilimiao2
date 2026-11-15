package cn.a10miaomiao.download

import com.a10miaomiao.bilimiao.comm.network.KitKatTlsHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadManager {
    private val mClient = KitKatTlsHelper.createOkHttpClientBuilder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private var call: Call? = null
    private var disposable: Disposable? = null
    private var downloadInfo: DownloadInfo? = null
    var callback: Callback? = null

    fun start(info: DownloadInfo, file: File, downloadedLength: Long = 0) {
        downloadInfo = info
        disposable = create(info, file, downloadedLength)
            .throttleLast(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ item ->
                callback?.onTaskRunning(item)
            }, { e ->
                info.status = -1
                callback?.onTaskError(info, e)
            }, {
                info.status = 1
                callback?.onTaskComplete(info)
            })
    }

    private fun create(info: DownloadInfo, file: File, downloadedLength: Long = 0) = Observable.create<DownloadInfo> {
        if (file.exists()) {
            info.progress = file.length()
        }
        var downloadLength = info.progress
        val request = Request.Builder()
            .url(info.url.replace("http://", "https://"))
        if (downloadLength > 0) {
            request.addHeader("RANGE", "bytes=$downloadLength-${info.size}")
        }
        downloadLength += downloadedLength
        for (keys in info.header.keys) {
            request.addHeader(keys, info.header[keys]!!)
        }
        call = mClient.newCall(request.build())
        val response = call!!.execute()
        if (!response.isSuccessful) {
            response.close()
            it.onError(IOException("Download HTTP ${response.code()}"))
            return@create
        }
        var fileOutputStream: FileOutputStream? = null
        try {
            val respBody = response.body() ?: throw IOException("Empty download body")
            val bis = BufferedInputStream(respBody.byteStream())
            fileOutputStream = FileOutputStream(file, true)
            val buffer = ByteArray(8192)
            var len: Int = bis.read(buffer)
            while (len != -1) {
                fileOutputStream.write(buffer, 0, len)
                downloadLength += len
                info.progress = downloadLength
                it.onNext(info)
                len = bis.read(buffer)
            }
            fileOutputStream.flush()
            it.onComplete()
        } catch (e: IOException) {
            e.printStackTrace()
            it.onError(e)
        } finally {
            try {
                fileOutputStream?.close()
            } catch (_: Throwable) {
            }
            response.close()
        }
    }

    fun cancel(): DownloadInfo? {
        call?.cancel()
        downloadInfo?.status = 2
        return downloadInfo
    }

    private fun getContentLength(downloadUrl: String): Long {
        val request = Request.Builder()
            .url(downloadUrl)
            .build()
        try {
            val call = mClient.newCall(request)
            val response = call.execute()
            if (response.isSuccessful) {
                val contentLength = response.body()?.contentLength() ?: -1L
                response.close()
                call.cancel()
                return if (contentLength == 0L) -1 else contentLength
            }
            response.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }

    interface Callback {
        fun onTaskRunning(info: DownloadInfo)
        fun onTaskComplete(info: DownloadInfo)
        fun onTaskError(info: DownloadInfo, error: Throwable)
    }
}
