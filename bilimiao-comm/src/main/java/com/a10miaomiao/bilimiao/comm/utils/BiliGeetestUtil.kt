package com.a10miaomiao.bilimiao.comm.utils

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import org.json.JSONObject

class BiliGeetestUtil(
    val activity: Activity,
    val lifecycle: Lifecycle,
    val gtCallBack: GTCallBack
) : LifecycleObserver {

    fun startCustomFlow() {
        Toast.makeText(
            activity,
            "Android 4.4 WebView 不支持极验人机验证，请优先使用「扫码登录」或「Cookie 登录」",
            Toast.LENGTH_LONG
        ).show()
    }

    fun onDestory() {
    }

    data class GT3ResultBean(
        val geetest_challenge: String,
        val geetest_seccode: String,
        val geetest_validate: String,
    )

    interface GTCallBack {
        suspend fun onGTDialogResult(result: GT3ResultBean): Boolean
        suspend fun getGTApiJson(): JSONObject?
    }
}
