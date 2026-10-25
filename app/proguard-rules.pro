# Mags release: R8 + resource shrinking.
# Keep serialization, OkHttp, crash UI, and Compose-adjacent enums.

-keepattributes *Annotation*, InnerClasses, Signature, Exception, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Kotlinx serialization
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    *;
}
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class nl.cita.magister.model.** { *; }
-keep class nl.cita.magister.dto.** { *; }
-keep class nl.cita.magister.export.** { *; }
-keep class nl.cita.magister.auth.** { *; }
-keep class nl.cita.magister.fixtures.** { *; }

# OkHttp / Okio (tests + optional engines)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keep class io.ktor.client.engine.cio.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Crash process + splash
-keep class nl.cita.magister.crash.** { *; }
-keep class nl.cita.magister.MainActivity { *; }
-keep class nl.cita.magister.CitaApp { *; }

# Glance / WorkManager / AndroidX Startup (InitializationProvider <init>)
-keep class nl.cita.magister.widget.** { *; }
-keep class nl.cita.magister.work.** { *; }
-keep class nl.cita.magister.live.** { *; }
-keep class androidx.startup.** { *; }
-keep class * extends androidx.startup.Initializer { *; }
-keepclassmembers class * extends androidx.startup.Initializer {
    public <init>();
}
-keep class androidx.profileinstaller.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.emoji2.** { *; }
-keep class androidx.core.splashscreen.** { *; }

-keep class kotlin.Metadata { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
