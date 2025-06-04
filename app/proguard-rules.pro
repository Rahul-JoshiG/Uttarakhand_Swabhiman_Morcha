# === General Optimization ===
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-ignorewarnings
-allowaccessmodification

# === Keep Application Classes ===
-keep class your.package.name.** { *; }

# === Obfuscate Class/Method/Field Names ===
-repackageclasses
-overloadaggressively
-flattenpackagehierarchy

# === Entry Points ===
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# === Keep annotations ===
-keepattributes *Annotation*

# === Keep Parcelable Models ===
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# === Firebase (Analytics, Crashlytics, Firestore, etc.) ===
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# === Retrofit / OkHttp ===
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keepattributes Signature
-keepattributes Exceptions

# === Gson (Model parsing) ===
-keep class com.google.gson.** { *; }
-keep class your.package.name.model.** { *; }
-keepattributes *Annotation*

# === For ViewBinding / DataBinding ===
-keep class **.databinding.*Binding { *; }
-keep class androidx.databinding.** { *; }

# === Kotlin Coroutines ===
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# === Prevent Logging in Release (Optional) ===
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# === Optional: Obfuscate package/class names aggressively ===
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable


# Hilt Core
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep class androidx.hilt.lifecycle.ViewModelFactoryModules$** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep class dagger.hilt.android.internal.managers.* { *; }
-keep class dagger.hilt.internal.** { *; }

# Dagger Core
-keep class dagger.** { *; }
-dontwarn dagger.internal.codegen.**

# For generated classes
-keep class * extends dagger.Component { *; }
-keep class * extends dagger.Subcomponent { *; }
-keep class * extends dagger.Module { *; }
-keep class * extends dagger.Binds { *; }

# Allow annotations
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class ** {
    @dagger.* <methods>;
    @dagger.* <fields>;
}

# Google Drive API
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.services.drive.**

# Google HTTP and OAuth
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

-keep class com.google.api.services.** { *; }
-dontwarn com.google.api.services.**

-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

# OAuth2 credentials (if using service accounts or user credentials)
-keep class com.google.auth.oauth2.** { *; }
-dontwarn com.google.auth.oauth2.**

# If using AndroidHttp and JacksonFactory
-keep class com.google.api.client.extensions.android.** { *; }
-dontwarn com.google.api.client.extensions.android.**

-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Support for JSON parsing
-keepattributes Signature
-keepattributes *Annotation*
