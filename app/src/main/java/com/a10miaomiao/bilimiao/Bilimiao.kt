package com.a10miaomiao.bilimiao

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.delegate.theme.ThemeDelegate
import com.a10miaomiao.bilimiao.comm.network.KitKatTlsHelper
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.loader.glide.GlideImageLoader
import net.mikaelzero.mojito.view.sketch.SketchImageLoadFactory

class Bilimiao : Application() {

    companion object {
        const val APP_NAME = "bilimiao"
        lateinit var commApp: BilimiaoCommApp
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        commApp = BilimiaoCommApp(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        KitKatTlsHelper.install()
        ThemeDelegate.setNightMode(this)
        Mojito.initialize(
            GlideImageLoader.with(this),
            SketchImageLoadFactory()
        )
        commApp.onCreate()
    }
}
