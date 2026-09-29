plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.android.library.compose)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.designsystem"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(libs.androidx.activity.compose)

    api(libs.androidx.compose.material.iconsCore)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.bundles.compose.md3)
    api(libs.androidx.compose.material3.adaptive)
    api(libs.androidx.compose.material3.adaptive.nav3)
    api(libs.androidx.compose.ui.util)

    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.navigation3.ui)

    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
