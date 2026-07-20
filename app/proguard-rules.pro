# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Jetpack Compose Rules
-keep class androidx.compose.ui.platform.AndroidComposeView { *; }

# Room Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.paging.**

# Moshi Rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
# Keep Moshi Kotlin Codegen files
-keep class *JsonAdapter { *; }
-keep class **_JsonAdapter { *; }

# Keep App Models
-keep class com.example.data.** { *; }
-keep @androidx.room.Entity class * { *; }

# Firebase Rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# General Serialization & Retrofit
-keepattributes Signature,InnerClasses,EnclosingMethod,Annotation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
