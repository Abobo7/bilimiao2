import com.google.protobuf.gradle.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.protobuf")
}

android {
    namespace = "com.a10miaomiao.bilimiao.comm"
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

    buildFeatures {
        buildConfig = true
    }

    sourceSets["main"].proto {
        srcDir("src/main/proto")
        include("**/*.proto")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.12.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.33.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                maybeCreate("java").apply {
                    option("lite")
                }
            }
            task.plugins {
                id("grpc") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.kodein.di:kodein-di-framework-android-x:7.12.0")
    implementation("com.github.bumptech.glide:glide:4.13.2")

    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:3.12.13")

    api("io.grpc:grpc-protobuf-lite:1.33.0")
    api("io.grpc:grpc-stub:1.33.0")
    implementation("javax.annotation:javax.annotation-api:1.2")

    implementation(project(":DanmakuFlameMaster"))

    testImplementation("junit:junit:4.13.2")
}
