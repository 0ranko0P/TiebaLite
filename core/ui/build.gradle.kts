plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.android.library.compose)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.ui"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:designsystem"))
    api(libs.bundles.coil3)
    api(libs.squareup.okhttp3)
    implementation(libs.androidx.appcompat)

    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
