plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "dev.giste.location"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Every resource in this module must start with "location_" (AGENTS.md §8). It has none yet.
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
    // :location is a reusable module: it depends only on the reusable :core and :geo
    // (AGENTS.md §4 and §4.1).
    implementation(project(":core"))
    // The public API exposes :geo value objects (GeoPoint, Distance, …), so consumers need it too.
    api(project(":geo"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // The public API exposes a location Flow, so consumers need coroutines too.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
