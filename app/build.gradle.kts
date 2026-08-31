plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.moto.pstar.nrswitcher.lsposed"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moto.pstar.nrswitcher.lsposed"
        minSdk = 30
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0-lineage23.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // 便于直接在手机上安装
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
    implementation("androidx.core:core-ktx:1.12.0")
}