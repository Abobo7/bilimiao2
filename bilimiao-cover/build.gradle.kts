plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "cn.a10miaomiao.bilimiao.cover"
    compileSdk = 35

    defaultConfig {
        minSdk = 19
        multiDexEnabled = true
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.github.bumptech.glide:glide:4.13.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:3.12.13")

    val mojitoVersion = "1.8.7"
    implementation("com.github.mikaelzero.mojito:mojito:$mojitoVersion")
    implementation("com.github.mikaelzero.mojito:SketchImageViewLoader:$mojitoVersion")
    implementation("com.github.mikaelzero.mojito:GlideImageLoader:$mojitoVersion")

    implementation(project(":bilimiao-comm"))

    testImplementation("junit:junit:4.13.2")
}
