plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
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

    // Every resource in this module must start with "settings_" (AGENTS.md §8).
    resourcePrefix = "settings_"

    testOptions {
        // Robolectric needs the merged resources to resolve strings in Compose UI tests.
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        // :settings shows the settings screen.
        compose = true
    }

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
    // The public API exposes the app settings as a Flow, so consumers need coroutines too.
    api(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // hiltViewModel(), to get the settings screen's ViewModel from Hilt in a composable.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Runs the Compose UI tests on the JVM.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
