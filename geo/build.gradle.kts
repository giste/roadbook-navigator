plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.giste.geo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 33
    }

    // Every resource in this module must start with "geo_" (AGENTS.md §8). It has none yet.
    resourcePrefix = "geo_"

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
    // :geo is the framework-free shared kernel: no project, Android or DI dependencies
    // (AGENTS.md §4 and §5.1).
    testImplementation(libs.junit)
}
