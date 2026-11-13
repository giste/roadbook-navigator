plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "dev.giste.odometer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Every resource in this module must start with "odometer_" (AGENTS.md §8). It has none yet.
    resourcePrefix = "odometer_"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    // Library modules must declare visibility explicitly (AGENTS.md §2, rule 4).
    explicitApi()
}

dependencies {
    // :odometer is a reusable module: it depends only on the reusable :core and :geo
    // (AGENTS.md §4 and §4.1). It has no UI: the app decides how to show the distances, so it
    // needs no Compose.
    implementation(project(":core"))
    // The public API exposes :geo value objects (Distance, Speed, …), so consumers need it too.
    api(project(":geo"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // The public API exposes the odometer state and settings as Flows, so consumers need
    // coroutines too.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
