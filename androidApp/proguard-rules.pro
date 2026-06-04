# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.mapchina.**$$serializer { *; }
-keepclassmembers class com.mapchina.** {
    *** Companion;
}
-keepclasseswithmembers class com.mapchina.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLDelight
-keep class com.mapchina.data.local.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# AMap
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-dontwarn com.amap.api.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# App models
-keep class com.mapchina.domain.model.** { *; }
-keep class com.mapchina.data.remote.** { *; }
