import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant

plugins {
    alias(libs.plugins.tblite.android.application)
    alias(libs.plugins.tblite.android.application.compose)
    alias(libs.plugins.tblite.hilt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

apply(from = "${rootProject.projectDir}/signing.gradle")

android {
    defaultConfig {
        applicationId = "com.huanchengfly.tieba.post"
        //noinspection OldTargetApi
        versionCode = 391033
        versionName = "4.0.0 Beta 5.2"

        // Configure custom runner to set up the Hilt test application
        testInstrumentationRunner = "$applicationId.TbLiteTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations.addAll(listOf("en", "zh-rCN"))
    }

    buildFeatures {
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

    packaging {
        resources.excludes += listOf(
            "META-INF/**",
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

    sourceSets {
        getByName("composeTracing") {
            assets.directories.add("src/benchmarkRelease/assets")
        }
    }

    namespace = "com.huanchengfly.tieba.post"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.ui.ExperimentalMediaQueryApi",
            "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=dev.chrisbanes.haze.ExperimentalHazeApi",
        )
    }
}

configurations.configureEach {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))

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
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.nav3)
    implementation(libs.androidx.compose.material.iconsCore)
    // Optional - Add full set of material icons
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.ui.util)
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
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.tracing)
    implementation(libs.bundles.paging3)

    // FastCSV
    implementation(libs.fastcsv)

    // WorkManager
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)
    androidTestImplementation(libs.androidx.work.testing)

    implementation(libs.google.gson)

    implementation(libs.haze.blur)

    // Coil
    implementation(libs.bundles.coil3)

    // Image Viewer
    implementation(libs.androidx.recyclerview)
    implementation(libs.iielse.imageviewer)
    implementation(libs.subsampling.image)

    implementation(libs.squareup.okhttp3)
    implementation(libs.squareup.retrofit2)
    implementation(libs.squareup.retrofit2.wire)

    implementation(libs.godaddy.colorpicker)
    implementation(libs.yalantis.ucrop)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
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
