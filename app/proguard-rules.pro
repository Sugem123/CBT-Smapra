# ExamBro ProGuard Rules

# Keep WebView JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, int);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin serialization
-keepattributes *Annotation*
-keepattributes InnerClasses

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Play Integrity
-keep class com.google.android.play.core.integrity.** { *; }

# Security Crypto
-keep class androidx.security.crypto.** { *; }

# Keep device admin receiver
-keep class com.smapra.exambro.security.ExamDeviceAdminReceiver { *; }
