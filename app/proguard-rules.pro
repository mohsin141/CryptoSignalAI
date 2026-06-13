-keep class com.cryptosignalai.data.model.** { *; }
-keep class com.cryptosignalai.data.remote.ai.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
