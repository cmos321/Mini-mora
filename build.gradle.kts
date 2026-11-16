// Archivo de configuración global del proyecto
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false // Firebase
    alias(libs.plugins.firebase.crashlytics) apply false // Crashlytics
}
