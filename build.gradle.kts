plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.10" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

extra.apply {
    set("compile_sdk_version", 35)
    set("target_sdk_version", 33)
    set("min_sdk_version", 19)
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("me.panpf:sketch-gif:2.7.1"))
                    .using(module("io.github.panpf.sketch:sketch-gif:2.7.1"))
            }
            // 锁定所有 AndroidX 与网络传递依赖至支持 Android 4.4.4 (API 19) 的稳定版本
            force("androidx.core:core:1.12.0")
            force("androidx.core:core-ktx:1.12.0")
            force("androidx.appcompat:appcompat:1.6.1")
            force("androidx.appcompat:appcompat-resources:1.6.1")
            force("androidx.activity:activity:1.8.2")
            force("androidx.activity:activity-ktx:1.8.2")
            force("androidx.fragment:fragment:1.6.2")
            force("androidx.fragment:fragment-ktx:1.6.2")
            force("androidx.lifecycle:lifecycle-common:2.6.2")
            force("androidx.lifecycle:lifecycle-runtime:2.6.2")
            force("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")
            force("androidx.lifecycle:lifecycle-livedata:2.6.2")
            force("androidx.lifecycle:lifecycle-livedata-core:2.6.2")
            force("androidx.savedstate:savedstate:1.2.1")
            force("androidx.savedstate:savedstate-ktx:1.2.1")
            force("androidx.navigation:navigation-common:2.5.3")
            force("androidx.navigation:navigation-common-ktx:2.5.3")
            force("androidx.navigation:navigation-runtime:2.5.3")
            force("androidx.navigation:navigation-runtime-ktx:2.5.3")
            force("androidx.navigation:navigation-fragment:2.5.3")
            force("androidx.navigation:navigation-fragment-ktx:2.5.3")
            force("androidx.navigation:navigation-ui:2.5.3")
            force("androidx.navigation:navigation-ui-ktx:2.5.3")
            force("androidx.recyclerview:recyclerview:1.2.1")
            force("androidx.constraintlayout:constraintlayout:2.1.4")
            force("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
            force("androidx.customview:customview:1.1.0")
            force("androidx.dynamicanimation:dynamicanimation:1.0.0")
            force("androidx.profileinstaller:profileinstaller:1.3.1")
            force("androidx.transition:transition:1.4.1")
            force("androidx.vectordrawable:vectordrawable:1.1.0")
            force("androidx.vectordrawable:vectordrawable-animated:1.1.0")
            force("androidx.annotation:annotation-experimental:1.3.1")
            force("androidx.media:media:1.6.0")
            force("com.google.android.material:material:1.6.1")
            force("com.squareup.okhttp3:okhttp:3.12.13")
            force("com.squareup.okio:okio:2.10.0")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
