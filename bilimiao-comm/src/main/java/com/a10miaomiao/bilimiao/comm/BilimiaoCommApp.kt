package com.a10miaomiao.bilimiao.comm

import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import androidx.annotation.RequiresApi
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.KitKatTlsHelper
import com.a10miaomiao.bilimiao.comm.utils.AESUtil
import com.a10miaomiao.bilimiao.comm.utils.MiaoEncryptDecrypt
import com.google.gson.Gson
import com.kongzue.dialogx.dialogs.PopTip
import java.io.File

class BilimiaoCommApp(
    val app: Application
) {
    var loginInfo: LoginInfo? = null
        private set

    private val authFilePath get() = app.filesDir.path + "/auth_hd"
    private val key = "Message Word"
    private var _bilibiliBuvid = ""

    companion object {
        lateinit var commApp: BilimiaoCommApp

        const val APP_NAME = "bilimiao"
    }

    fun onCreate() {
        commApp = this
        KitKatTlsHelper.install()
        ensureCookieSyncInitialized()
        readAuthInfo()

        PopTip.init(app)
    }

    @Suppress("DEPRECATION")
    fun ensureCookieSyncInitialized() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                CookieSyncManager.createInstance(app)
            } catch (_: Throwable) {
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun clearAllCookiesCompat(cookieManager: CookieManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Api21CookieHelper.clearCookies(cookieManager)
        } else {
            cookieManager.removeSessionCookie()
            cookieManager.removeAllCookie()
        }
    }

    @Suppress("DEPRECATION")
    private fun flushCookiesCompat(cookieManager: CookieManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Api21CookieHelper.flush(cookieManager)
        } else {
            try {
                CookieSyncManager.getInstance().sync()
            } catch (_: Throwable) {
            }
        }
    }

    fun setCookie(cookieInfo: LoginInfo.CookieInfo) {
        ensureCookieSyncInitialized()
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        clearAllCookiesCompat(cookieManager)
        cookieInfo.domains.forEach { domain ->
            cookieInfo.cookies.forEach { cookie ->
                cookieManager.setCookie(domain, cookie.getValue(domain))
            }
        }
        flushCookiesCompat(cookieManager)
    }

    fun setRawCookieString(rawCookie: String) {
        ensureCookieSyncInitialized()
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        clearAllCookiesCompat(cookieManager)
        val domains = listOf(".bilibili.com", ".biligame.com", ".bigfun.cn", ".bigfunapp.cn")
        val pairs = rawCookie.split(";").map { it.trim() }.filter { it.contains("=") }
        for (domain in domains) {
            for (pair in pairs) {
                cookieManager.setCookie(domain, "$pair; Domain=$domain; Path=/")
            }
        }
        flushCookiesCompat(cookieManager)
    }

    private fun getMiaoEncryptDecrypt(): MiaoEncryptDecrypt {
        val key = getBilibiliBuvid().toByteArray()
        return MiaoEncryptDecrypt(key)
    }

    fun saveAuthInfo(loginInfo: LoginInfo) {
        this.loginInfo = loginInfo
        val miaoED = getMiaoEncryptDecrypt()
        val jsonByteArray = Gson().toJson(loginInfo).toByteArray()
        val secretKey = AESUtil.getKey(key, app)
        val cipher = AESUtil.encrypt(miaoED.encrypt(jsonByteArray), secretKey)
        val file = File(authFilePath)
        file.writeBytes(cipher)
        loginInfo.cookie_info?.let { setCookie(it) }
    }

    private fun readAuthInfo(): LoginInfo? {
        try {
            val miaoED = getMiaoEncryptDecrypt()
            val secretKey = AESUtil.getKey(key, app)
            val file = File(authFilePath)
            if (!file.exists()) return null
            val cipher = file.readBytes()
            val jsonByteArray = miaoED.decrypt(AESUtil.decrypt(cipher, secretKey))
            val jsonStr = String(jsonByteArray)
            val loginInfo = Gson().fromJson(jsonStr, LoginInfo::class.java)
            this.loginInfo = loginInfo
            return loginInfo
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteAuth() {
        val file = File(authFilePath)
        file.delete()
        try {
            ensureCookieSyncInitialized()
            val cookieManager = CookieManager.getInstance()
            clearAllCookiesCompat(cookieManager)
            flushCookiesCompat(cookieManager)
        } catch (_: Throwable) {
        }
        this.loginInfo = null
    }

    fun getBilibiliBuvid(): String {
        if (_bilibiliBuvid.isNotBlank()) {
            return _bilibiliBuvid
        }
        val sp = app.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        var buvid = sp.getString("buvid", "")!!
        if (buvid.isBlank()) {
            buvid = ApiHelper.generateBuvid()
            sp.edit().putString("buvid", buvid).apply()
        }
        _bilibiliBuvid = buvid
        return buvid
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private object Api21CookieHelper {
        fun clearCookies(cookieManager: CookieManager) {
            cookieManager.removeSessionCookies(null)
            cookieManager.removeAllCookies(null)
        }

        fun flush(cookieManager: CookieManager) {
            cookieManager.flush()
        }
    }
}
