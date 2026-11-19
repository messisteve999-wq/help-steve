plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.steve.helpsteve"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.steve.helpsteve"
        minSdk = 23
        targetSdk = 28
        versionCode = 2
        versionName = "1.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
