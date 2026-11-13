plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.22-1.0.20" // 👈 Kotlin 1.9.22 ke hisab se updated KSP version
}

android {
    namespace = "com.amon.timer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vision.amon"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "1.1.5"

        vectorDrawables {
            useSupportLibrary = true
        }

        // 🔐 GitHub Secrets ki tijori se Google Sheet URL nikaal kar app ko dena
        val sheetUrl: String = System.getenv("GOOGLE_SHEET_URL") ?: ""
        buildConfigField("String", "GOOGLE_SHEET_URL", "\"$sheetUrl\"")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true // 🔑 Secret keys padhne ki permission chalu ki
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
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // UI & Design (Jetpack Compose)
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Navigation (Back Stack & Traffic Controller)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 🌐 Internet postman: Data ko Google Sheet tak pahunchane ke liye
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 🗄️ Room Database (Step 4 ke liye local storage)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
