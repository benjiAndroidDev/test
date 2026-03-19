# Ne pas toucher à Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Ne pas toucher à ML Kit (Scanner)
-keep class com.google.mlkit.** { *; }

# Ne pas toucher aux modèles de données
-keep class com.Upermarket.upermarket.** { *; }

# Ne pas toucher aux bibliothèques de navigation et compose
-keep class androidx.navigation.** { *; }
-keep class androidx.compose.** { *; }

# Garder les classes JSON pour GSON
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Fix R8 errors for firebase-crashlytics-buildtools
-dontwarn afu.org.checkerframework.**
-dontwarn com.google.firebase.crashlytics.buildtools.reloc.**
-dontwarn javax.servlet.**
-dontwarn org.ietf.jgss.**
-dontwarn org.checkerframework.**
