# ============================================================
# ProGuard rules for NexoHogar
# ============================================================

# ── Retrofit ────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# ── OkHttp ──────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Gson ────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── DTOs: Mantener todos los data class de red y modelo ─────
# data/model/
-keep class com.nexohogar.data.model.** { *; }
# data/remote/dto/
-keep class com.nexohogar.data.remote.dto.** { *; }
# domain/model/
-keep class com.nexohogar.domain.model.** { *; }

# ── Kotlin Serialization (si se usa en el futuro) ──────────
-keepclassmembers class kotlinx.serialization.** { *; }

# ── Firebase ────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }

# ── Mantener line numbers para crash reports ────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Compose ─────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── ML Kit ──────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
