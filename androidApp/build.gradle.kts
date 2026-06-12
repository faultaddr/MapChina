plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mapchina.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mapchina.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("io.insert-koin:koin-android:4.0.4")
    implementation("com.amap.api:3dmap:10.0.600")
    implementation("io.coil-kt.coil3:coil:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.10.0")
    testImplementation("androidx.compose.ui:ui-test-manifest:1.10.0")
    testImplementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    testImplementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:runner:1.6.2")
    testImplementation("junit:junit:4.13.2")
}
