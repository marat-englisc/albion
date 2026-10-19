plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 has built-in Kotlin; declaring the plugin here only pins the KGP version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
