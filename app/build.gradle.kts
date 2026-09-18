plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dayone.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dayone.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No signingConfig set -> Android Studio will prompt you to create/select
            // a keystore the first time you Build > Generate Signed APK.
            // For a quick unsigned/debug-signed test build, just use Build > Build APK(s)
            // which uses the debug config below.
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // CameraX
    val camerax = "1.3.4"
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

    // WorkManager (daily reminder scheduling)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Image loading in Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Video export: use platform MediaCodec/MediaMuxer directly (see video/ package) -
    // no extra library required, keeps the app fully offline and APK small.

    // Permissions helper
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
