package cn.xing.fileclouddisk.util

class GlobalExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            AppLogger.logCrash(throwable)
        } catch (e: Exception) {
            
        } finally {
            
            defaultHandler?.uncaughtException(thread, throwable)
                ?: run {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
        }
    }
}
