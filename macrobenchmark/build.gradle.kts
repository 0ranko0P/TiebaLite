import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kotlin-android")
    alias(libs.plugins.android.baselineprofile)
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.huanchengfly.tieba.macrobenchmark"

    defaultConfig {
        // Minimum supported version for Baseline Profiles.
        // On lower APIs, apps are fully AOT compile, therefore Baseline Profiles aren't needed.
        minSdk = 28
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    buildTypes {
        create("composeTracing") {
            defaultConfig.testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    // Note that your module name may have different name
    targetProjectPath = ":app"

    // Enable the benchmark to run separately from the app process
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.benchmark.junit)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.automator)

    // Adds dependencies to enable running Composition Tracing from Macrobenchmarks.
    // These dependencies are already included with Macrobenchmark, but we have them here to specify the version.
    // For more information on Composition Tracing, check https://developer.android.com/jetpack/compose/tooling/tracing.
    implementation(libs.androidx.tracing.perfetto)
    implementation(libs.androidx.tracing.perfetto.binary)
}
