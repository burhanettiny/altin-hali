plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Play Store için imzalama: anahtar bilgileri GitHub Secrets'tan ortam değişkeni olarak gelir (bkz. YAYIN.md).
val anahtarDosyasi: String? = System.getenv("ANAHTAR_DOSYASI")

android {
    namespace = "com.burhanettin.altinhali"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.burhanettin.altinhali"
        minSdk = 24
        targetSdk = 36
        // Her GitHub derlemesinde artar (Play Store her yüklemede daha büyük sürüm kodu ister)
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0) + 10
        versionName = "0.6"
    }

    buildFeatures {
        buildConfig = true
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

    // Reklam birimleri: deneme (debug) APK'sı HER ZAMAN Google'ın test reklamlarını gösterir,
    // böylece kendi telefonunda denerken AdMob hesabın risk altına girmez.
    // Gerçek reklamlar yalnızca Play Store'a giden imzalı sürümde (release) gösterilir.
    buildTypes {
        debug {
            buildConfigField("String", "ODULLU_BIRIM", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "GECIS_BIRIM", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            isMinifyEnabled = false
            if (anahtarDosyasi != null) signingConfig = signingConfigs.getByName("yayin")
            buildConfigField("String", "ODULLU_BIRIM", "\"ca-app-pub-1492578593198235/6137928629\"")
            buildConfigField("String", "GECIS_BIRIM", "\"ca-app-pub-1492578593198235/6033986847\"")
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
