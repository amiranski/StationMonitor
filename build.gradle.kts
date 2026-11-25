// Top-level build file: declares the Gradle plugins used by the modules (apply false = only put
// them on the build classpath, each module applies what it needs).
plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied anywhere: AGP 9 has built-in Kotlin support. Declaring it here pins the
    // Kotlin Gradle Plugin version that AGP uses to the one from libs.versions.toml.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
