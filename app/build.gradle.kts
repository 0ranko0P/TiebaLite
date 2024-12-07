import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wire)
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
    buildToolsVersion = libs.versions.buildToolsVersion.get()
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.huanchengfly.tieba.post"
        minSdk = libs.versions.minSdk.get().toInt()
        //noinspection OldTargetApi
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 390100
        versionName = "4.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations.addAll(listOf("en", "zh-rCN"))
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
        val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
        val gitVersion = gitVersionProvider.get()

        all {
            signingConfig =
                if (signingConfigs.any { it.name == "config" })
                    signingConfigs.getByName("config")
                else signingConfigs.getByName("debug")

            buildConfigField("String", "BUILD_GIT", "\"${gitVersion}\"")
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

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.jetbrains.annotations)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    // Required by Navigation Type-Safe
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    api(libs.wire.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.compose)

    // Compose Accompanist
    implementation(libs.accompanist.drawablepainter)
    implementation(project(":placeholder-material"))
    implementation(project(":insets-ui"))

    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.navigation)
    implementation(libs.androidx.compose.material.iconsCore)
    // Optional - Add full set of material icons
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.ui.util)
//    implementation "androidx.compose.material3:material3"
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    //AndroidX
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)

    implementation(libs.google.material)
    implementation(libs.google.gson)

    implementation(libs.database.litepal)
    implementation(libs.haze.blur)

    //Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.compose)
    implementation(libs.glide.okhttp3.integration)

    // Image Viewer
    implementation(libs.iielse.imageviewer)
    implementation(libs.subsampling.image)

    implementation(libs.squareup.okhttp3)
    implementation(libs.squareup.retrofit2)
    implementation(libs.squareup.retrofit2.wire)

    implementation(libs.colorful.sliders)
    implementation(libs.liyujiang.oadi)

    implementation(libs.godaddy.colorpicker)
    implementation(libs.yalantis.ucrop)

    //Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

abstract class GitVersionValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String = ByteArrayOutputStream().use { output ->
        execOperations.exec {
            commandLine("git", "branch", "--show-current")
            standardOutput = output
        }
        val branch = output.toString().trim()
        output.reset()

        execOperations.exec {
            commandLine("git", "rev-parse", "HEAD")
            standardOutput = output
        }
        val shortHash = output.toString().trim().substring(0..7)
        return@use "$branch#$shortHash"
    }
}
