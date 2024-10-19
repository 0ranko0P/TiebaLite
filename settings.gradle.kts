pluginManagement {
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
}

rootProject.name = "TiebaLite"
include(":app")
