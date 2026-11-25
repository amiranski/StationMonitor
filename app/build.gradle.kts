plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.amiran.stationmonitor"
    compileSdk {
        // The current Compose libraries need Android 17 QPR APIs (SDK 37.1+) at compile time.
        // compileSdk only affects which APIs we can call; it does not change minSdk or targetSdk.
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.amiran.stationmonitor"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"https://test-ucs-mock-api.vercel.app/\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // Lists the app's languages (from res/values-*) in Android 13+ Settings > Apps >
        // StationMonitor > Language, so Czech can be chosen for this app only.
        // The default language is declared in res/resources.properties.
        generateLocaleConfig = true
        // Ship only the languages the app is translated to. Libraries bring dozens of
        // translations; without this filter a German phone would show a mix of English app
        // texts and German library texts.
        localeFilters += setOf("en", "cs")
    }
}

room {
    // Room exports the database schema as JSON so future migrations can be written and tested.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // AndroidX core + Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Lifecycle: ViewModel, lifecycle-aware Flow collection
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation 3 (+ a ViewModel store per back stack entry)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Local cache
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Tooling for @Preview (debug builds only)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Unit tests (JVM, no emulator needed)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
