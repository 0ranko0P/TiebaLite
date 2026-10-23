package com.huanchengfly.tieba

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.util.Properties

internal fun Project.configureAndroidSigning(
    appExtension: ApplicationExtension,
) {
    with(appExtension) {
        val projectDirectory = isolated.rootProject.projectDirectory
        val propFile = projectDirectory.file("signing.properties").asFile
        signingConfigs {
            if (propFile.exists()) {
                val properties = Properties()
                propFile.inputStream().use { ins -> properties.load(ins) }

                create("config") {
                    storeFile = projectDirectory.file(properties.getProperty("KEYSTORE_FILE")).asFile
                    storePassword = properties.getProperty("KEYSTORE_PASSWORD")
                    keyAlias = properties.getProperty("KEY_ALIAS")
                    keyPassword = properties.getProperty("KEY_PASSWORD")

                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                    enableV4Signing = true
                }
            }
        }
    }
}
