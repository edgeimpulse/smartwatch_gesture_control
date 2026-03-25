# ── JNI ───────────────────────────────────────────────────────────────────────
# Keep native method names so the JNI linker can resolve them at runtime.
-keepclasseswithmembernames class * {
    native <methods>;
}
# MainActivity class path is baked into the C symbol names (e.g.
# Java_com_example_wizbulb_presentation_MainActivity_runInference).
# Renaming it would break the native library.
-keep class com.example.wizbulb.presentation.MainActivity { *; }

# ── Wear OS ───────────────────────────────────────────────────────────────────
-keep class com.google.android.wearable.** { *; }

# ── Crash diagnostics ─────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
