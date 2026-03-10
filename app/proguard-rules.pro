# ============================================================================
# SIGNAL App — ProGuard / R8 Rules
# ============================================================================
# These rules ensure R8 doesn't break runtime behavior for libraries that use
# reflection, annotation processing, or serialization.

# --- Stack traces -----------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin ----------------------------------------------------------------
# Keep Kotlin metadata for reflection (used by serialization, Hilt)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# --- Hilt / Dagger ---------------------------------------------------------
# Hilt generates code that R8 must not remove or rename.
# The Hilt Gradle plugin adds most rules automatically, but keep entry points:
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class *

# --- Room -------------------------------------------------------------------
# Room DAO interfaces and entity classes use annotations processed at compile time.
# R8 must not strip annotated fields or rename entity columns.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# --- kotlinx.serialization --------------------------------------------------
# Serialization uses @Serializable annotation + generated serializers.
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** serializer(...);
    kotlinx.serialization.KSerializer $$serializer(...);
}

# --- Ktor -------------------------------------------------------------------
# Ktor uses reflection for engine initialization and socket operations.
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- OkHttp -----------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- SIGNAL app models ------------------------------------------------------
# Enum types used in Room TypeConverters — must preserve valueOf().
-keepclassmembers enum dev.aiaerial.signal.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Compose navigation @Serializable route objects
-keep @kotlinx.serialization.Serializable class dev.aiaerial.signal.ui.navigation.** { *; }
