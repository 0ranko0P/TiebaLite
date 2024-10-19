import java.util.Properties

plugins {
    autowire(libs.plugins.com.android.application)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.compose.compiler)
    autowire(libs.plugins.kotlin.serialization)
    autowire(libs.plugins.kotlin.parcelize)
    autowire(libs.plugins.hilt.android)
    autowire(libs.plugins.kotlin.ksp)
    autowire(libs.plugins.com.squareup.wire)
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
    buildToolsVersion = "35.0.0"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.huanchengfly.tieba.post"
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 390100
        versionName = "4.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildFeatures {
        compose = true
    }
    signingConfigs {
        val properties = Properties()
        val propFile = project.rootProject.file("signing.properties")
        if (propFile.exists()) {
            properties.load(propFile.inputStream())
            create("config") {
                storeFile = project.rootProject.file(properties.getProperty("KEYSTORE_FILE"))
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
    buildTypes {
        all {
            signingConfig =
                if (signingConfigs.any { it.name == "config" })
                    signingConfigs.getByName("config")
                else signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            multiDexEnabled = true
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_11
        sourceCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }
    namespace = "com.huanchengfly.tieba.post"
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("compose_stability_configuration.txt")
}

dependencies {
    //Local Files
//    implementation fileTree(include: ["*.jar"], dir: "libs")

    val composeBom = platform(compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(net.swiftzer.semver.semver)
    implementation(godaddy.color.picker)

    implementation(airbnb.lottie)
    implementation(airbnb.lottie.compose)

    implementation(kotlinx.serialization.json)
    implementation(kotlinx.collections.immutable)

    implementation(androidx.media3.exoplayer)
    implementation(androidx.media3.ui)

    api(wire.runtime)

    implementation(hilt.android)
    ksp(hilt.compiler)
    implementation(androidx.hilt.navigation.compose)
    ksp(androidx.hilt.compiler)
    implementation(androidx.navigation.compose)

    implementation(accompanist.drawablepainter)
    implementation(accompanist.insets.ui)
    implementation(accompanist.systemuicontroller)
    implementation(accompanist.placeholder.material)

    implementation(compose.animation)
    implementation(compose.animation.graphics)
    implementation(compose.material)
    implementation(compose.material.navigation)
    implementation(compose.material.icons.core)
    // Optional - Add full set of material icons
    implementation(compose.material.icons.extended)
    implementation(compose.ui.util)
//    implementation "androidx.compose.material3:material3"

    // Android Studio Preview support
    implementation(compose.ui.tooling.preview)
    debugImplementation(compose.ui.tooling)

    // UI Tests
    androidTestImplementation(compose.ui.test.junit4)
    debugRuntimeOnly(compose.ui.test.manifest)

    implementation(github.oaid)

    implementation(org.jetbrains.annotations)

    implementation(kotlin.stdlib)
    implementation(kotlin.reflect)

    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.coroutines.android)

    implementation(androidx.lifecycle.runtime)
    implementation(androidx.lifecycle.viewmodel)
    implementation(androidx.lifecycle.viewmodel.compose)

    //AndroidX
    implementation(androidx.activity)
    implementation(androidx.activity.compose)
    implementation(androidx.appcompat)
    implementation(androidx.annotation)
    implementation(androidx.browser)
    implementation(androidx.constraintlayout)
    implementation(androidx.constraintlayout.compose)
    implementation(androidx.core)
    implementation(androidx.core.splashscreen)
    implementation(androidx.datastore.preferences)
    implementation(androidx.palette)
    implementation(androidx.window)
    implementation(androidx.startup.runtime)

    //Test
    testImplementation(junit.junit)
    androidTestImplementation(androidx.test.core)
    androidTestImplementation(androidx.test.espresso.core)

    //Glide
    implementation(glide.core)
    ksp(glide.ksp)
    implementation(glide.compose)
    implementation(glide.okhttp3.integration)

    implementation(google.material)

    implementation(okhttp3.core)
    implementation(retrofit2.core)
    implementation(retrofit2.converter.wire)

    implementation(google.gson)
    implementation(org.litepal.android.kotlin)

    implementation(subsampling.image)
    implementation(iielse.imageviewer)

    implementation(github.matisse)
    implementation(xx.permissions)
    implementation(com.github.yalantis.ucrop)
    implementation(colorful.sliders)
}
