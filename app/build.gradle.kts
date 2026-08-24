plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.patreze.rgbplaygestao"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.patreze.rgbplaygestao"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
}

kotlin {
    jvmToolchain(17)
}
