plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.amon.timer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vision.amon"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.4"

        vectorDrawables {
            useSupportLibrary = true
        }

        // 🔐 GitHub Secrets ki tijori se Google Sheet URL nikaal kar app ko dena
        val sheetUrl: String = System.getenv("GOOGLE_SHEET_URL") ?: ""
        buildConfigField("String", "GOOGLE_SHEET_URL", "\"$sheetUrl\"")
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
}
