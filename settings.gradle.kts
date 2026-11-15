pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "bilimiao"
include(":app")
include(":miao-binding", ":miao-binding-android")
include(":bilimiao-comm", ":bilimiao-cover")
include(":DanmakuFlameMaster")
include(":download")
