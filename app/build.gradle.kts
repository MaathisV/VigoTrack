plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.maathisv.vigotrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.maathisv.vigotrack"
        minSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    // Compose UI - Using the Bundle from TOML
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.ui)

    // Polar SDK & Coroutine Integration
    implementation(libs.polar.sdk)
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.kotlinx.coroutines.rx3)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Room Database - Using the Bundle from TOML
    implementation(libs.bundles.room)
    implementation(libs.androidx.documentfile)
    ksp(libs.androidx.room.compiler) // KSP must stay separate from bundles

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // QR Code Scanner (Google Play Services)
    implementation(libs.play.services.code.scanner)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)

    // Instrumented Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug Tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}