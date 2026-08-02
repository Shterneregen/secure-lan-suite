# Experimental desktop shrinking and bytecode optimization configuration.
# Keep the first optimization experiment conservative and reproducible.
-optimizationpasses 1
# ProGuard 7.8.2 can specialize Kotlin method descriptors without updating all
# corresponding bytecode types. This produces unverifiable methods in Coroutines
# and Compose Runtime and makes the jpackage launcher fail before main.
-optimizations !method/specialization/*
# Obfuscation remains intentionally disabled for this stage.
-dontobfuscate

# jpackage launcher entry point.
-keep public class com.shterneregen.securelan.desktop.compose.ComposeDesktopMainKt {
    public static void main(java.lang.String[]);
}

# Preserve JNI method names and every class used by their descriptors.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Skiko and webrtc-java are JNI boundaries. Native code can call Java members that
# have no JVM-visible call site, so retain these two narrowly scoped bridge APIs.
-keep,includedescriptorclasses class org.jetbrains.skiko.** { *; }
-keep,includedescriptorclasses class org.jetbrains.skia.** { *; }
-keep,includedescriptorclasses class dev.onvoid.webrtc.** { *; }

# Preserve runtime metadata used by Kotlin, Compose, reflection and serialization.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,AnnotationDefault

# Keep AndroidX elements explicitly marked as reflective/runtime entry points.
-keep,allowobfuscation @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# kotlinx.serialization discovers generated serializers through companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# AtomicFieldUpdater and coroutine service hooks refer to these elements by name.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# ProGuard copies non-class resources; retain service descriptor directories as well.
-keepdirectories META-INF/services

# Addressed optional/false-positive references from the exact resolved dependencies:
# - coroutines carries Android/build-tool-only annotations that are not needed on desktop;
# - JBR API invokes MethodHandle signature-polymorphic methods with descriptors that
#   ProGuard cannot match to the varargs declaration present in java.base.
-dontwarn android.annotation.SuppressLint
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn com.jetbrains.JBR
