plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.hilt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.wire)
    id("kotlinx-serialization")
}

wire {
    sourcePath {
        srcDir("src/main/protos")
    }

    kotlin {
        android = true
    }
}

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.huanchengfly.tieba.post.core.network"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:common"))
    api(libs.wire.runtime)
    api(libs.google.gson)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.squareup.okhttp3)
    implementation(libs.squareup.retrofit2)
    implementation(libs.squareup.retrofit2.wire)

    testImplementation(libs.kotlinx.coroutines.test)
}