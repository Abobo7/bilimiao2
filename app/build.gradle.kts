import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.a10miaomiao.bilimiao"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.a10miaomiao.bilimiao"
        minSdk = 19
        targetSdk = 33
        versionCode = 123
        versionName = "2.5.3-kitkat"
        multiDexEnabled = true

        flavorDimensions += listOf("default")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    fun createManifestPlaceholders(channelName: String) = mapOf(
        "APP_CHANNEL_VALUE" to channelName,
    )

    productFlavors {
        create("github") {
            dimension = "default"
            addManifestPlaceholders(createManifestPlaceholders("Github"))
        }
        create("coolapk") {
            dimension = "default"
            applicationId = "cn.a10miaomiao.bilimiao.dev"
            addManifestPlaceholders(createManifestPlaceholders("Coolapk"))
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "**/*.proto"
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.media:media:1.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.kodein.di:kodein-di-framework-android-x:7.12.0")

    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("me.zhanghai.android.foregroundcompat:library:1.0.2")
    implementation("com.drakeet.drawer:drawer:1.0.3")
    implementation("de.maxr1998:modernandroidpreferences:2.2.0")

    // Splitties View DSL
    val splittiesVersion = "3.0.0-beta06"
    implementation("com.louiscad.splitties:splitties-fun-pack-android-base:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-fun-pack-android-base-with-views-dsl:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-fun-pack-android-appcompat:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-fun-pack-android-appcompat-with-views-dsl:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-fun-pack-android-material-components:$splittiesVersion")
    implementation("com.louiscad.splitties:splitties-fun-pack-android-material-components-with-views-dsl:$splittiesVersion")

    // 图片预览 Mojito
    val mojitoVersion = "1.8.7"
    implementation("com.github.mikaelzero.mojito:mojito:$mojitoVersion")
    implementation("com.github.mikaelzero.mojito:SketchImageViewLoader:$mojitoVersion")
    implementation("com.github.mikaelzero.mojito:GlideImageLoader:$mojitoVersion")

    // 播放器：Media3 1.3.1 (最后一个官方支持 Android 4.4 / API 19 的稳定版) + GSYVideoPlayer
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-decoder:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-datasource:$media3Version")
    implementation("com.github.CarGuo.GSYVideoPlayer:gsyVideoPlayer-java:v8.3.5-release-jitpack")

    // 网络、TLS 1.2/1.3 (Conscrypt)、JSON、二维码生成、图片加载
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:3.12.13")
    implementation("io.grpc:grpc-protobuf-lite:1.33.0")
    implementation("com.github.bumptech.glide:glide:4.13.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.13.2")

    implementation(project(":bilimiao-comm"))
    implementation(project(":bilimiao-cover"))
    implementation(project(":download"))
    implementation(project(":miao-binding"))
    implementation(project(":miao-binding-android"))
    implementation(project(":DanmakuFlameMaster"))

    testImplementation("junit:junit:4.13.2")
}

// 重排主 classes.dex 闭包，确保 Android 4.4.4 Dalvik 在 MultiDex.install 前零类缺失
tasks.matching { it.name.startsWith("mergeDex") || it.name.startsWith("mergeLibDex") || it.name.startsWith("mergeProjectDex") }.configureEach {
    if (!name.startsWith("mergeDex")) return@configureEach
    val dexTask = this
    doLast {
        val sdkDir = android.sdkDirectory
        val isWin = System.getProperty("os.name").lowercase().contains("win")
        val d8Bin = File(sdkDir, "build-tools/35.0.0/" + if (isWin) "d8.bat" else "d8")
        val androidJar = File(sdkDir, "platforms/android-${android.compileSdk}/android.jar")
        val rulesFile = file("multidex-config.pro")
        if (!d8Bin.exists() || !androidJar.exists() || !rulesFile.exists()) {
            throw GradleException("Missing d8/android.jar/multidex-config.pro for KitKat main-dex repack")
        }
        dexTask.outputs.files.files.filter { it.isDirectory }.forEach { outDir ->
            val dexFiles = outDir.listFiles()?.filter { it.name.endsWith(".dex") }?.sortedBy { it.name } ?: emptyList()
            if (dexFiles.isNotEmpty()) {
                val tmpOut = File(layout.buildDirectory.asFile.get(), "tmp/${dexTask.name}_kitkat_maindex")
                if (tmpOut.exists()) tmpOut.deleteRecursively()
                tmpOut.mkdirs()
                val cmd = mutableListOf(
                    d8Bin.absolutePath,
                    if (dexTask.name.lowercase().contains("debug")) "--debug" else "--release",
                    "--min-api", "19",
                    "--lib", androidJar.absolutePath,
                    "--main-dex-rules", rulesFile.absolutePath,
                    "--output", tmpOut.absolutePath
                ).apply { addAll(dexFiles.map { it.absolutePath }) }
                val pb = ProcessBuilder(cmd)
                pb.environment()["JAVA_HOME"] = System.getProperty("java.home")
                pb.redirectErrorStream(true)
                val proc = pb.start()
                val logText = proc.inputStream.bufferedReader().readText()
                val code = proc.waitFor()
                if (code != 0) {
                    throw GradleException("d8 main-dex repack failed ($code):\n$logText")
                }
                dexFiles.forEach { it.delete() }
                tmpOut.listFiles()?.filter { it.name.endsWith(".dex") }?.forEach { f ->
                    Files.copy(f.toPath(), File(outDir, f.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
