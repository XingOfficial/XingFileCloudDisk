package cn.xing.fileclouddisk.network

import cn.xing.fileclouddisk.util.AppLogger
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var currentBaseUrl: String = ""
    private var retrofit: Retrofit? = null
    private var service: ApiService? = null

    
    @Volatile
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private fun normalize(url: String): String {
        var u = url.trim()
        if (u.isEmpty()) u = "http://localhost/"
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/")) u = "$u/"
        return u
    }

    fun getService(baseUrl: String): ApiService {
        val normalized = normalize(baseUrl)
        if (service == null || normalized != currentBaseUrl) {
            currentBaseUrl = normalized

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val authInterceptor = okhttp3.Interceptor { chain ->
                val original = chain.request()
                val token = authToken
                val request = if (!token.isNullOrBlank()) {
                    original.newBuilder().addHeader("Authorization", "Bearer $token").build()
                } else {
                    original
                }
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            service = retrofit!!.create(ApiService::class.java)
        }
        return service!!
    }
}

fun <T> Response<T>.errorMessageOrDefault(default: String = "请求失败"): String {
    return try {
        val errBody = errorBody()?.string()
        if (errBody.isNullOrBlank()) return default
        val parsed = Gson().fromJson(errBody, ApiErrorResponse::class.java)
        parsed?.message ?: parsed?.error ?: default
    } catch (e: Exception) {
        AppLogger.logWarn("ApiClient", "解析错误响应体失败", e)
        default
    }
}
