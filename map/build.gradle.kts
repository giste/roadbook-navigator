plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    // :map is a reusable module, so its namespace is dev.giste.<module> (AGENTS.md §4.1).
    namespace = "dev.giste.map"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Every resource in this module must start with "map_" (AGENTS.md §8).
    resourcePrefix = "map_"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // Robolectric needs the merged resources to resolve strings in Compose UI tests.
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        // :map shows the map.
        compose = true
    }
}

kotlin {
    // Library modules must declare visibility explicitly (AGENTS.md §2, rule 4).
    explicitApi()
}

dependencies {
    // :map is a reusable feature module: it depends only on the reusable :core and :geo
    // (AGENTS.md §4 and §4.1).
    implementation(project(":core"))
    // The public API exposes :geo value objects (LocationFix), so consumers need it too.
    api(project(":geo"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // The public API exposes the location and settings inputs as Flows, so consumers need
    // coroutines too.
    api(libs.kotlinx.coroutines.core)

    // VTM (mapsforge's OpenGL vector map) draws offline mapsforge .map files. vtm-android brings
    // the Android MapView; its native renderer comes as one jar per ABI. arm64-v8a and
    // armeabi-v7a cover devices; x86_64 covers the emulator.
    implementation(libs.vtm)
    implementation(libs.vtm.android)
    implementation(libs.vtm.themes)
    runtimeOnly(variantOf(libs.vtm.android) { classifier("natives-arm64-v8a") })
    runtimeOnly(variantOf(libs.vtm.android) { classifier("natives-armeabi-v7a") })
    runtimeOnly(variantOf(libs.vtm.android) { classifier("natives-x86_64") })

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // hiltViewModel(), to get the map's ViewModel from Hilt in a composable.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Runs the Compose UI tests on the JVM.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
