package cn.xing.fileclouddisk

import android.app.Application
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.GlobalExceptionHandler

class CloudDiskApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        
        AppLogger.init(this)

        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(defaultHandler))

        AppLogger.logInfo("CloudDiskApplication", "App启动")
    }
}
