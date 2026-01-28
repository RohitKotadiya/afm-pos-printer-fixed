# Add project specific ProGuard rules here.
-keep class net.posprinter.** { *; }
-keepclassmembers class net.posprinter.** { *; }

# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Gson
-keep class com.achyutamfruitam.pos.BillData { *; }
-keep class com.achyutamfruitam.pos.BillLineItem { *; }
