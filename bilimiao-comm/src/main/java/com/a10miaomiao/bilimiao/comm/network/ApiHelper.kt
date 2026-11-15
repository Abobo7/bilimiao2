package com.a10miaomiao.bilimiao.comm.network

import android.net.Uri
import android.os.Build
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

object ApiHelper {

    const val BUILD_VERSION = 1450000
    const val BILI_APP_VERSION = "1.45.0"

    // Android HD APP_KEY（支持二维码扫码登录与全部平板端接口）
    const val APP_KEY = "dfca71928277209b"
    const val APP_SECRET = "b5475a8825547a4fc26c7d518eaaa02e"

    const val REFERER = "https://www.bilibili.com/"
    const val APP_BASE = "https://app.bilibili.com/"
    const val GRPC_BASE = "https://app.bilibili.com/"

    val USER_AGENT: String
        get() = "Mozilla/5.0 BiliDroid/$BILI_APP_VERSION (bbcallen@gmail.com) " +
            "os/android model/${Build.MODEL} mobi_app/android_hd " +
            "build/$BUILD_VERSION channel/bili innerVer/$BUILD_VERSION " +
            "osVer/${Build.VERSION.RELEASE} network/2"

    fun getTimeSpen() = Date().time / 1000

    fun getUUID(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    fun generateBuvid(): String {
        val uuid = getUUID() + getUUID()
        return "XY" + uuid.substring(0, 35).uppercase()
    }

    fun getSing(url: String, secret: String): String {
        var str = url.substring(url.indexOf("?", 4) + 1)
        val list = str.split("&").toMutableList()
        list.sort()
        str = with(StringBuilder()) {
            list.forEach { item ->
                append(if (isNotEmpty()) "&" else "")
                append(item)
            }
            return@with toString()
        }
        return getMD5(str + secret)
    }

    fun getMD5(info: String): String {
        try {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(info.toByteArray())
            val encryption = md5.digest()
            val strBuf = StringBuffer()
            for (i in encryption.indices) {
                val hex = Integer.toHexString(0xff and encryption[i].toInt())
                if (hex.length == 1) {
                    strBuf.append("0").append(hex)
                } else {
                    strBuf.append(hex)
                }
            }
            return strBuf.toString()
        } catch (e: NoSuchAlgorithmException) {
            return ""
        } catch (e: UnsupportedEncodingException) {
            return ""
        }
    }

    fun getSing(params: Map<String, String?>, secret: String): String {
        val paramsStr = urlencode(params, true)
        return getMD5(paramsStr + secret)
    }

    fun urlencode(params: Map<String, String?>, isSort: Boolean = false): String {
        val list = params.map {
            if (!it.value.isNullOrEmpty()) {
                "${Uri.encode(it.key)}=${Uri.encode(it.value)}"
            } else {
                ""
            }
        }.toMutableList()
        if (isSort) {
            list.sort()
        }
        return StringBuilder().apply {
            list.forEach { item ->
                if (item.isNotEmpty()) {
                    append(if (isNotEmpty()) "&" else "")
                    append(item)
                }
            }
        }.toString()
    }

    fun addAccessKeyAndMidToParams(params: MutableMap<String, String?>) {
        BilimiaoCommApp.commApp.loginInfo?.token_info?.let {
            params["access_key"] = it.access_token
            if (!params.contains("mid")) {
                params["mid"] = it.mid.toString()
            }
        }
    }

    fun createParams(vararg pairs: Pair<String, String?>): MutableMap<String, String?> {
        return createParams(mutableMapOf(*pairs))
    }

    fun createParams(base: Map<String, String?>): MutableMap<String, String?> {
        val params = mutableMapOf<String, String?>()
        params.putAll(base)
        params["appkey"] = APP_KEY
        params["build"] = BUILD_VERSION.toString()
        params["buvid"] = BilimiaoCommApp.commApp.getBilibiliBuvid()
        params["mobi_app"] = "android_hd"
        params["platform"] = "android"
        params["ts"] = getTimeSpen().toString()
        if (params["notoken"]?.isEmpty() != false) {
            addAccessKeyAndMidToParams(params)
        }
        params["sign"] = getSing(params, APP_SECRET)
        return params
    }
}
