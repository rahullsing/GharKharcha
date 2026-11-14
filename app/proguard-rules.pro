# GharKharcha R8 / ProGuard Rules

# Preserve line numbers and source file names for crash reporting / stack traces
-keepattributes SourceFile,LineNumberTable

# Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Kotlinx Serialization (Navigation 3 type-safe routes)
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# AndroidX Biometric
-keep class androidx.biometric.** { *; }
