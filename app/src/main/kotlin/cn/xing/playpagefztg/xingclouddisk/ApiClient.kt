package cn.xing.playpagefztg.xingclouddisk

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    fun interface Callback<T> {
        fun onResult(result: T?, error: String?)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun request(path: String, method: String, body: JSONObject?, cb: Callback<JSONObject>) {
        val url = Config.API_BASE + path
        val builder = Request.Builder().url(url)
        builder.header("Content-Type", "application/json")
        // 跳过 zrok 公开分享链接的浏览器警告拦截页，否则收到的不是 JSON 而是 HTML
        builder.header("skip_zrok_interstitial", "true")

        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((body?.toString() ?: "{}").toRequestBody("application/json".toMediaTypeOrNull()))
            "PATCH" -> builder.patch((body?.toString() ?: "{}").toRequestBody("application/json".toMediaTypeOrNull()))
            "DELETE" -> builder.delete()
        }

        client.newCall(builder.build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                cb.onResult(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val resp = response.body?.string() ?: ""
                try {
                    val obj = JSONObject(resp)
                    if (obj.has("error")) {
                        cb.onResult(null, obj.optString("error", "未知错误"))
                    } else {
                        cb.onResult(obj, null)
                    }
                } catch (e: Exception) {
                    cb.onResult(null, "解析错误: $resp")
                }
            }
        })
    }

    fun uploadFile(owner: String, filename: String, mimeType: String, base64Content: String, cb: Callback<JSONObject>) {
        try {
            val body = JSONObject()
            body.put("ownerUsername", owner)
            body.put("filename", filename)
            body.put("mimeType", mimeType)
            body.put("content", base64Content)
            request("/upload", "POST", body, cb)
        } catch (e: Exception) {
            cb.onResult(null, e.message)
        }
    }
}
