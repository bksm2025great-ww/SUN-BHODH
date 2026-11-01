# 1. दोनों संभावित पैकेजों को 100% सुरक्षित रखना
-keep class com.amon.timer.** { *; }
-keepclassmembers class com.amon.timer.** { *; }

-keep class com.vision.amon.** { *; }
-keepclassmembers class com.vision.amon.** { *; }

# 2. अगर कोई क्लास न भी मिले तो बिल्ड न रुके (नो वार्निंग/एरर)
-ignorewarnings
-dontwarn com.amon.timer.**
-dontwarn com.vision.amon.**

# 3. नेटवर्क, कोरूटीन्स और JSON पार्सिंग के नियम
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
