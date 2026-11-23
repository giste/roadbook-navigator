plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Generates the serializers of the type-safe navigation routes.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "dev.giste.roadbooknavigator"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "dev.giste.roadbooknavigator"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // Robolectric needs the merged resources to resolve strings in Compose UI tests.
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":location"))
    implementation(project(":map"))
    implementation(project(":odometer"))
    implementation(project(":roadbook"))
    // Stores and provides the settings of every module (AGENTS.md §4.4).
    implementation(project(":settings"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // hiltViewModel(), to get the dashboard's ViewModel from Hilt in a composable.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    // Moves between the dashboard and the settings screen, with type-safe routes.
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Runs the Compose UI tests on the JVM.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Runs the app's real Hilt graph in the integration tests, on Robolectric.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}