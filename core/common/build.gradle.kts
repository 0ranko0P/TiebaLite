plugins {
    alias(libs.plugins.tblite.jvm.library)
    alias(libs.plugins.tblite.hilt)
    id("kotlinx-serialization")
}

dependencies {
    api(libs.google.gson)

    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
