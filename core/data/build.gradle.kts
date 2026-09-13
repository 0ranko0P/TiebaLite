plugins {
    alias(libs.plugins.tblite.android.library)
    alias(libs.plugins.tblite.hilt)
}

android {
    namespace = "com.huanchengfly.tieba.post.core.data"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:database"))
    api(project(":core:network"))

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
