package cn.xing.fileclouddisk.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("clouddisk_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_SERVER_URL, value).apply()

    var username: String
        get() = sp.getString(KEY_USERNAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_USERNAME, value).apply()

    
    var token: String
        get() = sp.getString(KEY_TOKEN, "") ?: ""
        set(value) = sp.edit().putString(KEY_TOKEN, value).apply()

    var avatar: String
        get() = sp.getString(KEY_AVATAR, "") ?: ""
        set(value) = sp.edit().putString(KEY_AVATAR, value).apply()

    var quota: Long
        get() = sp.getLong(KEY_QUOTA, 0L)
        set(value) = sp.edit().putLong(KEY_QUOTA, value).apply()

    val isLoggedIn: Boolean
        get() = username.isNotBlank()

    val isAdmin: Boolean
        get() = username == "admin"

    fun logout() {
        sp.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_AVATAR)
            .remove(KEY_QUOTA)
            .remove(KEY_TOKEN)
            .apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_QUOTA = "quota"
        private const val KEY_TOKEN = "token"
    }
}
