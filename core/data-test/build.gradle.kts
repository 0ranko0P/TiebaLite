plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.hilt)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.data.test"
}

dependencies {
    api(project(":core:data"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android.testing)
}
