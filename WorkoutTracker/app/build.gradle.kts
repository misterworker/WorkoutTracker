/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    kotlin("kapt")
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.workoutwrecker.workouttracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.workoutwrecker.workouttracker"
        minSdk = 29
        targetSdk = 34
        versionCode = 17
        versionName = "1.0196"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation(libs.glide)
    implementation("com.airbnb.android:lottie:5.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.activity)
    implementation(libs.integrity)
    implementation(libs.firebase.appcheck.playintegrity)
    kapt(libs.glide.compiler) // If you use annotation processors
    implementation(libs.androidx.security.crypto)

    // For selecting images from the gallery
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions:21.0.0")
    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("net.zetetic:android-database-sqlcipher:4.5.0")
    implementation("androidx.sqlite:sqlite:2.1.0")
    implementation ("androidx.sqlite:sqlite-framework:2.1.0")


    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.android.billingclient:billing-ktx:7.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")

    // For instrumentation tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.52")

    // For local unit tests
    testImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptTest("com.google.dagger:hilt-compiler:2.52")


    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kapt {
    correctErrorTypes = true
}

