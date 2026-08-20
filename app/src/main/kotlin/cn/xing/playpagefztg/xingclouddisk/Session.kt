package cn.xing.playpagefztg.xingclouddisk

import android.content.Context
import android.content.SharedPreferences

object Session {
    fun set(ctx: Context, key: String, value: String) {
        getPrefs(ctx).edit().putString(key, value).apply()
    }

    fun get(ctx: Context, key: String): String? {
        return getPrefs(ctx).getString(key, null)
    }

    fun clear(ctx: Context) {
        getPrefs(ctx).edit().clear().apply()
    }

    fun setUser(ctx: Context, user: String) {
        set(ctx, "user", user)
    }

    fun getUser(ctx: Context): String? {
        return get(ctx, "user")
    }

    fun setQuota(ctx: Context, quota: Long) {
        getPrefs(ctx).edit().putLong("quota", quota).apply()
    }

    fun getQuota(ctx: Context): Long {
        return getPrefs(ctx).getLong("quota", 0)
    }

    private fun getPrefs(ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)
    }
}
