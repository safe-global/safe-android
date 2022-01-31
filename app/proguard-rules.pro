# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/frederico/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontobfuscate
-keepattributes *Annotation*

####################################################################################################
# Crash Reporting
####################################################################################################
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

####################################################################################################
# Spongycastle
####################################################################################################
-dontwarn javax.naming.**

####################################################################################################
# Okio
####################################################################################################
-dontwarn okio.**
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

####################################################################################################
# Okhttp
####################################################################################################
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

####################################################################################################
# Moshi
####################################################################################################
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
# Codegen related files
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

-keep @interface pm.gnosis.heimdall.data.adapters.HexNumber
-keep @interface pm.gnosis.heimdall.data.adapters.DecimalNumber
-keep @interface pm.gnosis.heimdall.data.adapters.BigDecimalNumber

-keep interface io.gnosis.data.models.** { *; }
-keep class io.gnosis.data.models.** { *; }

-keep interface io.gnosis.safe.notifications.models.** { *; }
-keep class io.gnosis.safe.notifications.models.** { *; }
####################################################################################################
# Retrofit
####################################################################################################
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain service method parameters.
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

####################################################################################################
# BouncyCastle
####################################################################################################
-keep class org.bouncycastle.** { *; }

####################################################################################################
# Intercom
####################################################################################################
-keep class io.intercom.android.** { *; }
-keep class com.intercom.** { *; }
