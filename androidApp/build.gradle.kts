plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mapchina.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mapchina.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("io.insert-koin:koin-android:4.0.4")
}
