plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "edu.cs4730.arcoresimpledemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "edu.cs4730.arcoresimpledemo"
        minSdk = 32
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
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
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.arcore)
    implementation(libs.material)
}