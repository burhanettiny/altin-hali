plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Play Store için imzalama: anahtar bilgileri GitHub Secrets'tan ortam değişkeni olarak gelir (bkz. YAYIN.md).
val anahtarDosyasi: String? = System.getenv("ANAHTAR_DOSYASI")

android {
    namespace = "com.burhanettin.altinhali"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.burhanettin.altinhali"
        minSdk = 24
        targetSdk = 35
        // Her GitHub derlemesinde artar (Play Store her yüklemede daha büyük sürüm kodu ister)
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0) + 10
        versionName = "0.5"
    }

    signingConfigs {
        if (anahtarDosyasi != null) {
            create("yayin") {
                storeFile = file(anahtarDosyasi)
                storePassword = System.getenv("ANAHTAR_SIFRESI")
                keyAlias = System.getenv("ANAHTAR_ADI")
                keyPassword = System.getenv("ANAHTAR_SIFRESI")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (anahtarDosyasi != null) signingConfig = signingConfigs.getByName("yayin")
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
    // Google AdMob reklamları ve gizlilik izni (GDPR) formu
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
}
