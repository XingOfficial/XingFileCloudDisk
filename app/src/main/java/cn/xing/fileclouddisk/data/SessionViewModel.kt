package cn.xing.fileclouddisk.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.xing.fileclouddisk.network.ApiClient
import cn.xing.fileclouddisk.util.AppLogger
import kotlinx.coroutines.launch

class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    companion object {
        const val FIXED_SERVER_URL = "https://x947.share.zrok.io"
    }

    var serverUrl by mutableStateOf(FIXED_SERVER_URL)
        private set

    var username by mutableStateOf(prefs.username)
        private set

    var avatar by mutableStateOf(prefs.avatar)
        private set

    var quota by mutableStateOf(prefs.quota)
        private set

    var token by mutableStateOf(prefs.token)
        private set

    val isLoggedIn: Boolean
        get() = username.isNotBlank()

    val isAdmin: Boolean
        get() = username == "admin"

    val hasServer: Boolean
        get() = true

    

    init {
        
        if (token.isNotBlank()) {
            ApiClient.setAuthToken(token)
        }
    }

    fun onLoginSuccess(username: String, quota: Long, avatar: String, token: String) {
        prefs.username = username
        prefs.quota = quota
        prefs.avatar = avatar
        prefs.token = token
        this.username = username
        this.quota = quota
        this.avatar = avatar
        this.token = token
        ApiClient.setAuthToken(token)
    }

    fun updateAvatar(avatar: String) {
        prefs.avatar = avatar
        this.avatar = avatar
    }

    fun updateQuota(quota: Long) {
        prefs.quota = quota
        this.quota = quota
    }

    fun logout() {
        
        viewModelScope.launch {
            try {
                ApiClient.getService(serverUrl).logout()
            } catch (e: Exception) {
                AppLogger.logWarn("SessionViewModel", "退出登录时后端令牌吊销失败", e)
            }
        }
        prefs.logout()
        username = ""
        avatar = ""
        quota = 0L
        token = ""
        ApiClient.setAuthToken(null)
    }
}