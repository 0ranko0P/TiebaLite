plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.android.library.compose)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))

    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
