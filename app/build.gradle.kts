plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.moveai"
    compileSdk = 36 // Updated to match the latest libraries

    defaultConfig {
        applicationId = "com.example.moveai"
        minSdk = 24 // Excellent for CameraX and ML Kit
        targetSdk = 34
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // The proper way to enable ViewBinding in Kotlin DSL
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")

    // ML Kit & CameraX (AI Trainer)
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation("com.google.mlkit:pose-detection:17.0.0")
    // Added this for highly accurate body angle calculations
    implementation("com.google.mlkit:pose-detection-accurate:17.0.0")

    // Retrofit, Gson, Lottie, Glide, and Ripple Animation
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("com.skyfishjy.ripplebackground:library:1.0.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // AMap 3D Map
    implementation("com.amap.api:3dmap:9.8.3")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}