package com.a10miaomiao.bilimiao.comm.network

import android.os.Build
import android.util.Base64
import bilibili.metadata.MetadataOuterClass
import bilibili.metadata.device.DeviceOuterClass
import bilibili.metadata.fawkes.Fawkes
import bilibili.metadata.locale.LocaleOuterClass
import bilibili.metadata.network.NetworkOuterClass
import bilibili.metadata.restriction.RestrictionOuterClass
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp

object BiliGRPCConfig {

    val channel = "bilibili140"
    val networkType = 2
    val networkTF = 0
    val networkOid = "46007"

    val buvid get() = BilimiaoCommApp.commApp.getBilibiliBuvid()

    val mobileApp = "android_hd"
    val platform = "android"
    val envorienment = "prod"

    // 5 为安卓平板 APP
    var appId = 5

    val region = "CN"
    val language = "zh"

    fun getFawkesreqBin(): String {
        val msg = Fawkes.FawkesReq.newBuilder()
            .setAppkey(mobileApp)
            .setEnv(envorienment)
            .build()
        return toBase64(msg.toByteArray())
    }

    fun getMetadataBin(accessToken: String): String {
        val msg = MetadataOuterClass.Metadata.newBuilder()
            .setAccessKey(accessToken)
            .setMobiApp(mobileApp)
            .setBuild(ApiHelper.BUILD_VERSION)
            .setChannel(channel)
            .setBuvid(buvid)
            .setPlatform(platform)
            .build()
        return toBase64(msg.toByteArray())
    }

    fun getDeviceBin(): String {
        val msg = DeviceOuterClass.Device.newBuilder()
            .setAppId(appId)
            .setMobiApp(mobileApp)
            .setBuild(ApiHelper.BUILD_VERSION)
            .setChannel(channel)
            .setBuvid(buvid)
            .setPlatform(platform)
            .setBrand(Build.BRAND ?: "Xiaomi")
            .setModel(Build.MODEL ?: "MI PAD")
            .setOsver(Build.VERSION.RELEASE ?: "4.4.4")
            .build()
        return toBase64(msg.toByteArray())
    }

    fun getNetworkBin(): String {
        val msg = NetworkOuterClass.Network.newBuilder()
            .setType(NetworkOuterClass.NetworkType.WIFI)
            .setOid(networkOid)
            .build()
        return toBase64(msg.toByteArray())
    }

    fun getRestrictionBin(): String {
        val msg = RestrictionOuterClass.Restriction.newBuilder()
            .build()
        return toBase64(msg.toByteArray())
    }

    fun getLocaleBin(): String {
        val cLocale = LocaleOuterClass.LocaleIds.newBuilder()
            .setLanguage(language)
            .setRegion(region)
            .build()
        val sLocale = LocaleOuterClass.LocaleIds.newBuilder()
            .setLanguage(language)
            .setRegion(region)
            .build()
        val msg = LocaleOuterClass.Locale.newBuilder()
            .setCLocale(cLocale)
            .setSLocale(sLocale)
            .build()
        return toBase64(msg.toByteArray())
    }

    fun getSystemUserAgent(): String {
        return System.getProperty("http.agent") ?: "Mozilla/5.0"
    }

    fun toBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
