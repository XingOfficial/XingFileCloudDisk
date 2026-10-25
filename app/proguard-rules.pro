# isMinifyEnabled = true 时生效

# ---------- 关键：保留注解相关attribute ----------
# 之前只保留了 Signature/*Annotation*，但R8 full mode下这还不够，
# Retrofit在运行时要反射读取方法/参数上的 @GET @POST @Body @Path @Query 等注解，
# 如果下面这些attribute被裁掉，Retrofit会找不到注解，报"接口不存在"一类的错误。
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ---------- Retrofit 官方推荐规则 ----------
-keep,allowobfuscation,allowoptimization interface cn.xing.fileclouddisk.network.ApiService

# 保留接口上所有带 retrofit2.http 注解的方法及其注解本身
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# suspend函数编译后会多一个 kotlin.coroutines.Continuation 参数，
# Retrofit 需要能正确识别它，保留协程相关类避免被裁剪
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlin.coroutines.Continuation

# ---------- Gson ----------
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# TypeToken 的泛型信息必须保留，否则Gson反序列化List<T>等会出问题
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ---------- 关键：所有网络数据模型类 ----------
# 保留字段名 + 不做任何裁剪/内联，避免JSON字段名和序列化对不上
-keep,allowobfuscation,allowshrinking class cn.xing.fileclouddisk.network.** { *; }
-keepclassmembers class cn.xing.fileclouddisk.network.** {
    <fields>;
    <init>(...);
}

# ---------- Kotlin元数据（Gson/Retrofit反射kotlin类需要）----------
-keep class kotlin.Metadata { *; }
-keepattributes KotlinMetadata
-dontwarn kotlin.**

# ---------- Compose ----------
-dontwarn androidx.compose.**
