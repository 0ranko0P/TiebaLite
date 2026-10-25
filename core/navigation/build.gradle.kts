plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.navigation"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.savedstate.compose)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
}
