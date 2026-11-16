plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services) // Firebase (lee google-services.json)
    alias(libs.plugins.firebase.crashlytics) // Crashlytics
}

android {
    namespace = "com.example.mini8firebase"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mini8firebase"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Configuración para compilar con Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(platform(libs.firebase.bom)) // Versiones de Firebase
    implementation(libs.firebase.firestore) // 1. Cloud Firestore
    implementation(libs.firebase.database) // 2. Realtime Database
    implementation(libs.firebase.auth) // 3. Authentication
    implementation(libs.firebase.storage) // 4. Cloud Storage
    implementation(libs.firebase.messaging) // 5. Cloud Messaging
    implementation(libs.firebase.analytics) // 7. Analytics
    implementation(libs.firebase.crashlytics) // 8. Crashlytics
    implementation(libs.glide) // Carga de imágenes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
