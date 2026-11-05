import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY") ?: ""

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.nuietsiit"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.nuietsiit"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.annotation)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.zxing.android.embedded) // QR del justificante (Secretaría) y escáner ISBN (Biblioteca)
    implementation(libs.biometric) // firmar solicitudes con la huella (Secretaría)
    implementation(libs.generativeai) // SDK de Gemini AI
    implementation(libs.guava) // Interoperabilidad de ListenableFuture en Java
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}
