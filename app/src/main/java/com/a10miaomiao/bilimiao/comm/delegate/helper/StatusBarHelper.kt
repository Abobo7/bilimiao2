package com.a10miaomiao.bilimiao.comm.delegate.helper

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate

class StatusBarHelper(
    val activity: Activity,
) {

    var isShowNavigation = true
        set(value) {
            field = value
            update()
        }
    var isShowStatus = true
        set(value) {
            field = value
            update()
        }
    var isLightStatusBar = true
        set(value) {
            field = value
            update()
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Api21WindowHelper.applyTransparentStatusBar(activity.window)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4 KitKat 半透明沉浸式状态栏
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    fun update() {
        var uiFlags = if (isShowStatus) {
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        uiFlags = uiFlags or 0x00001000
        if (!isShowNavigation) {
            uiFlags = uiFlags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
        val lightMode = isLightStatusBar && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && lightMode) {
            uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        activity.window.decorView.systemUiVisibility = uiFlags
        setMiuiStatusBarDarkMode(activity.window, lightMode && isShowStatus)
    }

    private fun setMiuiStatusBarDarkMode(window: Window, darkMode: Boolean) {
        try {
            val clazz = window.javaClass
            val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
            val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
            val darkModeFlag = field.getInt(layoutParams)
            val extraFlagField = clazz.getMethod("setExtraFlags", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            extraFlagField.invoke(window, if (darkMode) darkModeFlag else 0, darkModeFlag)
        } catch (_: Throwable) {
        }
    }

    fun getStatusBarHeight(): Int {
        var statusBarHeight = 0
        val resourceId: Int = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = activity.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private object Api21WindowHelper {
        fun applyTransparentStatusBar(window: Window) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }
}
