import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.wire)
}

apply(from = "${rootProject.projectDir}/signing.gradle")

wire {
    sourcePath {
        srcDir("src/main/protos")
    }

    kotlin {
        android = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.huanchengfly.tieba.post"
        minSdk = libs.versions.minSdk.get().toInt()
        //noinspection OldTargetApi
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 390899
        versionName = "4.0.0 Beta 4.2"
        // Configure custom runner to set up the Hilt test application
        testInstrumentationRunner = "$applicationId.TbLiteTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations.addAll(listOf("en", "zh-rCN"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        val epochSecond = Instant.now(Clock.systemUTC()).epochSecond
        val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
        val gitVersion = gitVersionProvider.get()

        all {
            // Apply signing config from signing.properties, see ../signing.gradle
            signingConfigs.findByName("config")?.let { signingConfig = it }
            // Replaced with buildConfigField#BUILD_GIT
            vcsInfo.include = false
            buildConfigField("String", "BUILD_GIT", "\"${gitVersion}\"")
            buildConfigField("long", "BUILD_TIME", "${epochSecond}L")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "compose-rules.pro",
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            multiDexEnabled = true
        }

        // Variant for benchmark, auto-selected by Macrobenchmark
        create("benchmarkRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".benchmark"
            isProfileable = true
            proguardFile("benchmark-rules.pro")
        }

        // Variant for composition tracing, auto-selected by benchmarkComposeTracing in Macrobenchmark
        create("composeTracing") {
            initWith(getByName("benchmarkRelease"))
            proguardFiles.removeIf { it.name == "compose-rules.pro" } // Keep tracing API
        }

        // Variant for CI, profileable
        create("ci") {
            initWith(getByName("benchmarkRelease"))
            applicationIdSuffix = ".ci"
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs.addAll(
                "-Xcontext-parameters",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi",
                "-opt-in=dev.chrisbanes.haze.ExperimentalHazeApi",
            )
        }
    }

    packaging {
        resources.excludes += listOf(
            "META-INF/**",
            "kotlin/**",
            "kotlin-tooling-metadata.json", // Unneeded: See KT-48019
            "**.bin"
        )
    }

    lint {
        disable.addAll(listOf("LocalContextGetResourceValueCall", "UseKtx"))
        checkReleaseBuilds = false
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
    }

    namespace = "com.huanchengfly.tieba.post"
}

configurations.configureEach {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")

    stabilityConfigurationFiles.addAll(
        rootProject.layout.projectDirectory.file("compose_stability_configuration.txt")
    )
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
    implementation(libs.androidx.media3.ui.compose)

    api(libs.wire.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.compose)

    // Compose Accompanist
    implementation(libs.accompanist.drawablepainter)
    implementation(project(":placeholder"))

    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.graphics)

    // Material compose
    implementation(libs.bundles.compose.md3)
    implementation(libs.androidx.compose.material.iconsCore)
    // Optional - Add full set of material icons
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewModel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(project(":material-color-utilities"))

    //AndroidX
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.tracing)
    implementation(libs.bundles.paging3)

    // WorkManager
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)
    androidTestImplementation(libs.androidx.work.testing)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    androidTestImplementation(libs.androidx.room.testing)

    implementation(libs.google.gson)

    implementation(libs.haze.blur)

    //Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.compose)
    implementation(libs.glide.okhttp3.integration)

    // Image Viewer
    implementation(libs.androidx.recyclerview)
    implementation(libs.iielse.imageviewer)
    implementation(libs.subsampling.image)

    implementation(libs.squareup.okhttp3)
    implementation(libs.squareup.retrofit2)
    implementation(libs.squareup.retrofit2.wire)

    implementation(libs.liyujiang.oadi)

    implementation(libs.godaddy.colorpicker)
    implementation(libs.yalantis.ucrop)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Startup Profile & Baseline Profile
    // baselineProfile(project(":macrobenchmark"))
}

abstract class GitVersionValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String = ByteArrayOutputStream().use { output ->
        execOperations.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = output
        }
        output.toString().trim()
    }
}
