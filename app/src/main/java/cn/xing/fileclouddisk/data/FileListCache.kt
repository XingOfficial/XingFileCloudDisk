package cn.xing.fileclouddisk.data

import android.content.Context
import cn.xing.fileclouddisk.network.FileItem
import cn.xing.fileclouddisk.util.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FileListCache(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("clouddisk_file_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    
    fun save(username: String, items: List<FileItem>) {
        try {
            sp.edit()
                .putString(keyItems(username), gson.toJson(items))
                .putLong(keyTime(username), System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            
            AppLogger.logWarn("FileListCache", "写入文件列表缓存失败", e)
        }
    }

    
    fun load(username: String): CachedFiles? {
        val json = sp.getString(keyItems(username), null) ?: return null
        val savedAt = sp.getLong(keyTime(username), 0L)
        return try {
            val type = object : TypeToken<List<FileItem>>() {}.type
            val items: List<FileItem> = gson.fromJson(json, type) ?: emptyList()
            CachedFiles(items, savedAt)
        } catch (e: Exception) {
            AppLogger.logWarn("FileListCache", "解析文件列表缓存失败", e)
            null
        }
    }

    private fun keyItems(username: String) = "files_$username"
    private fun keyTime(username: String) = "files_time_$username"

    data class CachedFiles(val items: List<FileItem>, val savedAtMillis: Long)
}
