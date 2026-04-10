import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

// ── SEC-01: Leer API Key desde local.properties ──────────────────────────────
val localProperties = Properties()
rootProject.file("local.properties").let { file ->
    if (file.exists()) {
        localProperties.load(FileInputStream(file))
    }
}

android {
    namespace = "com.nexohogar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nexohogar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${localProperties.getProperty("SUPABASE_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "https://fpsdkpurugviwygfuljp.supabase.co")}\""
        )
        // ── URL específica para Retrofit (REST API) ──────────────────────────
        buildConfigField(
            "String",
            "SUPABASE_REST_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "https://fpsdkpurugviwygfuljp.supabase.co")}/rest/v1/\""
        )
    }

    buildTypes {
        release {
            // SEC-03: Habilitar ofuscacion y shrink en release
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

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true   // Necesario para BuildConfig.SUPABASE_KEY
    }

    // Con Kotlin 2.0+ el Compose compiler se configura via el plugin kotlin-compose,
    // ya no se necesita composeOptions { kotlinCompilerExtensionVersion }.

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation (XML & Compose)
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // ── WorkManager (notificaciones en background) ────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ── SEC-02: EncryptedSharedPreferences ─────────────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")

    // ── OTH-01: Dependencias para tests unitarios ─────────────────────────
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // ML Kit Text Recognition (OCR offline)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // ── Firebase ──
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // ── Supabase Auth SDK (session persistence + auto-refresh) ────────────
    // Reemplaza la gestión manual de tokens (EncryptedSharedPreferences)
    // con persistencia en DataStore + refresh automático. Login una sola vez.
    implementation("io.github.jan-tennert.supabase:auth-kt:3.3.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.3.0")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")

    // Coil — carga de imágenes async para Compose
    implementation("io.coil-kt:coil-compose:2.7.0")

    // DataStore (storage para supabase-kt SessionManager)
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Kotlinx serialization runtime (serialización de UserSession en DataStore)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.compose.material:material-icons-extended")
}
