plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.stepup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.stepup"
        minSdk = 35
        targetSdk = 36
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
}

dependencies {
    // ספריות כלליות
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // ניהול חיי אפליקציה וגרפים
    implementation("androidx.lifecycle:lifecycle-process:2.8.3")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Firebase & AI Setup (השינוי המרכזי) ---
    // 1. הגדרת ה-BOM (Bill of Materials)
    implementation(platform(libs.firebase.bom))

    // 2. שירותי Firebase (ללא גרסאות - ה-BOM דואג לזה)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)

    // 3. ספריות עזר ל-Gemini
    implementation(libs.guava)
    implementation(libs.reactive.streams)
    // ------------------------------------------

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}