package com.a10miaomiao.bilimiao.page.auth

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object Api21WebCookieHelper {
    fun enableThirdPartyCookies(webView: WebView) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }
}
