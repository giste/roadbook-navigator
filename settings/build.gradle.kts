plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "dev.giste.roadbooknavigator.settings"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Every resource in this module must start with "settings_" (AGENTS.md §8). It has none yet.
    resourcePrefix = "settings_"

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
    // :settings may depend on :core, :geo and the feature modules (AGENTS.md §4): it implements
    // every feature's settings source.
    implementation(project(":core"))
    implementation(project(":geo"))
    implementation(project(":location"))
    implementation(project(":odometer"))
    implementation(project(":roadbook"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Persists the settings of every module (AGENTS.md §7).
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
