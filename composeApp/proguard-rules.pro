# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-dontwarn org.slf4j.impl.StaticLoggerBinder

# If you're using a specific Secp256k1 library,
-keep class fr.acinq.secp256k1.** { *; }
# Keep OpenVPN API classes
-keep class de.blinkt.openvpn.api.** { *; }
-keep interface de.blinkt.openvpn.api.** { *; }

# Keep any classes that might be using the OpenVPN API
-keep class * implements de.blinkt.openvpn.api.IOpenVPNAPIService { *; }
-keep class * implements de.blinkt.openvpn.api.IOpenVPNStatusCallback { *; }

# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}