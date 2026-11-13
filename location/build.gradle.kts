plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "dev.giste.roadbooknavigator.location"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Every resource in this module must start with "location_" (AGENTS.md §8).
    resourcePrefix = "location_"

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
    // Feature modules may depend only on :core (AGENTS.md §4).
    implementation(project(":core"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // The public API exposes a location Flow, so consumers need coroutines too.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
