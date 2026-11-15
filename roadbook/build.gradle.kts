plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    // :roadbook is a reusable module, so its namespace is dev.giste.<module> (AGENTS.md §4.1).
    namespace = "dev.giste.roadbook"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Every resource in this module must start with "roadbook_" (AGENTS.md §8).
    resourcePrefix = "roadbook_"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // :roadbook draws the waypoints and moderators of the roadbook.
        compose = true
    }
}

kotlin {
    // Library modules must declare visibility explicitly (AGENTS.md §2, rule 4).
    explicitApi()
}

dependencies {
    // :roadbook is a reusable feature module: it depends only on the reusable :core and :geo
    // (AGENTS.md §4 and §4.1).
    implementation(project(":core"))
    // The public API will expose :geo value objects (Distance, …), so consumers need it too.
    api(project(":geo"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // The public API will expose roadbook events and the partial distance input as Flows, so
    // consumers need coroutines too.
    api(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
