# Keep RFID library intact - prevents obfuscation issues
-keep class com.rscja.** { *; }
-keep interface com.rscja.** { *; }
-keep enum com.rscja.** { *; }

# Keep UHF UART implementation 
-keep class com.rscja.deviceapi.RFIDWithUHFUART { *; }
-keep class com.rscja.deviceapi.entity.UHFTAGInfo { *; }
-keep class com.rscja.deviceapi.interfaces.** { *; }

# From older rules - keep all these packages
-dontwarn com.imagealgorithmlab.**
-dontwarn com.hsm.**
-dontwarn com.rscja.deviceapi.**
-keep class com.hsm.** {*; }
-keep class com.rscja.deviceapi.** {*; }

# Export data JARs (from old config)
-keep class common.** { *; }
-keep class jxl.** { *; }
-keep class aavax.**{*;}
-keep class com.**{*;}
-keep class org.**{*;}
-keep class schemaorg_apache_xmlbeans.system.**{*;}
-keep interface org.**{*;}

# General Android rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Expo module definitions
-keep class expo.modules.** { *; }
-keep class com.facebook.react.bridge.** { *; } 