# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve the line number information for deobfuscation in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep data models and Room entities intact to prevent schema/query issues
-keep class com.example.data.** { *; }

# Keep androidx room generated files and annotations
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase { *; }

# Keep Moshi and Retrofit models if obfuscated
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep Moshi adapter classes
-keep class *JsonAdapter { *; }
-keep class com.example.data.**JsonAdapter { *; }
