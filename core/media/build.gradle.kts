plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.android.library.compose)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.media"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:ui"))
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)

    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
