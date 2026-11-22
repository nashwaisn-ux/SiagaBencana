# ProGuard & R8 Optimization Rules for SiagaBencana

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve data models for Gson serialization
-keep class com.example.siagabencana.model.** { *; }
-keepclassmembers class com.example.siagabencana.model.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Retrofit
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory {
    public <init>();
}
-keepclassmembers class * implements com.google.gson.JsonSerializer {
    public <init>();
}
-keepclassmembers class * implements com.google.gson.JsonDeserializer {
    public <init>();
}
-dontwarn com.google.gson.**

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-dontwarn com.bumptech.glide.**

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
