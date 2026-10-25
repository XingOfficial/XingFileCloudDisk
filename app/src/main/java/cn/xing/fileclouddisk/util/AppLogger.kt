package cn.xing.fileclouddisk.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object AppLogger {

    private const val TAG = "AppLogger"
    private const val LOG_DIR_NAME = "logs"
    private const val MAX_LOG_DAYS = 7L 

    private val dateFileFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)

    
    private val ioExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var appContext: Context? = null

    
    fun init(context: Context) {
        appContext = context.applicationContext
        cleanOldLogs()
    }

    private fun logDir(): File? {
        val ctx = appContext ?: return null
        
        
        val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        val dir = File(base, LOG_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun currentLogFile(): File? {
        val dir = logDir() ?: return null
        val name = "app_${dateFileFormat.format(Date())}.log"
        return File(dir, name)
    }

    
    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
        write("INFO", tag, message, null)
    }

    
    fun logWarn(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        write("WARN", tag, message, throwable)
    }

    
    fun logError(tag: String, throwable: Throwable, message: String = "捕获到异常") {
        Log.e(tag, message, throwable)
        write("ERROR", tag, message, throwable)
    }

    
    fun logCrash(throwable: Throwable) {
        Log.e(TAG, "应用发生未捕获异常，即将写入崩溃日志", throwable)
        write("CRASH", "GlobalExceptionHandler", buildDeviceInfo(), throwable, forceSync = true)
    }

    private fun buildDeviceInfo(): String {
        val ctx = appContext
        val versionName = try {
            ctx?.packageManager?.getPackageInfo(ctx.packageName, 0)?.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        return "App版本=$versionName, 型号=${Build.MANUFACTURER} ${Build.MODEL}, " +
            "Android版本=${Build.VERSION.RELEASE}(SDK ${Build.VERSION.SDK_INT})"
    }

    private fun write(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
        forceSync: Boolean = false
    ) {
        val ctx = appContext
        if (ctx == null) {
            Log.w(TAG, "AppLogger尚未初始化，无法写入日志文件")
            return
        }
        val task = Runnable {
            try {
                val file = currentLogFile() ?: return@Runnable
                file.appendText(buildString {
                    append(timeFormat.format(Date()))
                    append(" [")
                    append(level)
                    append("] ")
                    append(tag)
                    append(": ")
                    append(message)
                    append('\n')
                    if (throwable != null) {
                        val sw = StringWriter()
                        throwable.printStackTrace(PrintWriter(sw))
                        append(sw.toString())
                        append('\n')
                    }
                })
            } catch (e: Exception) {
                
                Log.e(TAG, "写入日志文件失败", e)
            }
        }
        
        if (forceSync) task.run() else ioExecutor.execute(task)
    }

    private fun cleanOldLogs() {
        ioExecutor.execute {
            try {
                val dir = logDir() ?: return@execute
                val cutoff = System.currentTimeMillis() - MAX_LOG_DAYS * 24 * 60 * 60 * 1000
                dir.listFiles()?.forEach { f ->
                    if (f.isFile && f.lastModified() < cutoff) {
                        f.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理过期日志失败", e)
            }
        }
    }
}
