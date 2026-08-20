package cn.xing.playpagefztg.xingclouddisk

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * base64 内容和真实文件之间的转换工具：
 * - saveToDownloads：把 base64 解码写进系统"下载"目录，是真正的文件字节，不是假下载。
 * - writeToCacheAndGetUri：解码写到应用缓存目录，再用 FileProvider 包装成 content:// Uri，
 *   给"用外部 App 打开/播放"这种场景用。
 */
object FileStorageUtils {

    /** 把 base64 内容解码后真正写进系统"下载"目录。 */
    @Throws(Exception::class)
    fun saveToDownloads(context: Context, filename: String, mimeType: String, base64Content: String) {
        val bytes = Base64.decode(base64Content, Base64.DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri: Uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("无法在系统下载目录创建文件")
            context.contentResolver.openOutputStream(uri).use { out ->
                if (out == null) throw Exception("无法写入系统下载目录")
                out.write(bytes)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val outFile = File(downloadsDir, filename)
            FileOutputStream(outFile).use { out -> out.write(bytes) }
        }
    }

    /** 把 base64 内容解码写到应用缓存目录，返回一个可以给别的 App 用的 content:// Uri。 */
    @Throws(Exception::class)
    fun writeToCacheAndGetUri(context: Context, filename: String, base64Content: String): Uri {
        val bytes = Base64.decode(base64Content, Base64.DEFAULT)
        val cacheDir = File(context.cacheDir, "shared_files")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val outFile = File(cacheDir, filename)
        FileOutputStream(outFile).use { out -> out.write(bytes) }
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outFile)
    }
}
