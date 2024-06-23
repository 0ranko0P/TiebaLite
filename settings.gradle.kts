pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/repository/public")
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
}

rootProject.name = "TiebaLite"
include(":app")
