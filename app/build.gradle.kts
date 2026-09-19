plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dayone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dayone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.2"
    }

    signingConfigs {
        // The release build is signed with the checked-in keystore below rather than
        // a machine-local one. Android will only install an update over an existing
        // install when both APKs are signed with the *same* key, so keeping one key in
        // the repo is what lets every future release update in place - photos, streaks
        // and settings all survive. It uses the standard Android debug-key credentials,
        // which are public constants, so no secret is stored anywhere in this project.
        getByName("debug") {
            val checkedIn = rootProject.file("keystore/dayone.keystore")
            if (checkedIn.exists()) {
                storeFile = checkedIn
            }
        }
    }

    buildTypes {
        release {
            // Left off deliberately: this APK is built and shipped without a device in
            // the loop, and R8 stripping is the classic source of "works in debug, crashes
            // in release" bugs. The extra size is irrelevant for a sideloaded app.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            // NOTE: no applicationIdSuffix. Debug and release share one package name so
            // that a debug build from Android Studio and a release APK are the same app
            // and the same data directory, instead of two side-by-side installs.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // CameraX
    val camerax = "1.4.0"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // ML Kit face detection (fully on-device, no network calls)
    implementation("com.google.mlkit:face-detection:16.1.7")

    // EXIF rotation handling for captured JPEGs
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Room (local DB only)
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // Image loading in Compose
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Video export uses the platform's MediaCodec/MediaMuxer + OpenGL ES directly
    // (see the video/ package) - no third party library, no network access.

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
