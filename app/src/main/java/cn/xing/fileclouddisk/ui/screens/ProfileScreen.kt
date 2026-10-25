package cn.xing.fileclouddisk.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.ApiClient
import cn.xing.fileclouddisk.network.AvatarUploadRequest
import cn.xing.fileclouddisk.network.UserInfoResponse
import cn.xing.fileclouddisk.network.errorMessageOrDefault
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.humanSize
import cn.xing.fileclouddisk.util.progressOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    session: SessionViewModel,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    var userInfo by remember { mutableStateOf<UserInfoResponse?>(null) }
    var usedSize by remember { mutableStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        scope.launch {
            try {
                val api = ApiClient.getService(session.serverUrl)
                val infoResp = api.getUserInfo(session.username)
                if (infoResp.isSuccessful) {
                    userInfo = infoResp.body()
                    session.updateQuota(infoResp.body()?.quota ?: session.quota)
                } else {
                    errorMsg = infoResp.errorMessageOrDefault("获取用户信息失败")
                }
                val filesResp = api.getFiles(session.username)
                if (filesResp.isSuccessful) {
                    usedSize = filesResp.body()?.items
                        ?.filter { it.ownerUsername == session.username }
                        ?.sumOf { it.size } ?: 0L
                }
            } catch (e: Exception) {
                AppLogger.logError(TAG, e, "加载个人信息失败")
                errorMsg = "网络错误：${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingAvatar = true
            scope.launch {
                try {
                    val base64 = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                        val bytes = input?.readBytes() ?: ByteArray(0)
                        input?.close()
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                    val api = ApiClient.getService(session.serverUrl)
                    val resp = api.uploadAvatar(AvatarUploadRequest(session.username, base64))
                    if (resp.isSuccessful) {
                        session.updateAvatar(base64)
                        load()
                    } else {
                        errorMsg = resp.errorMessageOrDefault("上传头像失败")
                    }
                } catch (e: Exception) {
                    AppLogger.logError(TAG, e, "上传头像失败")
                    errorMsg = "网络错误：${e.message}"
                } finally {
                    uploadingAvatar = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val avatarB64 = session.avatar
                if (avatarB64.isNotBlank()) {
                    val bitmap = remember(avatarB64) {
                        try {
                            val bytes = Base64.decode(avatarB64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            AppLogger.logError(TAG, e, "解码头像图片失败")
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = "头像", modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "用户", modifier = Modifier.size(48.dp))
                    }
                } else {
                    Icon(Icons.Default.Person, contentDescription = "用户", modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { avatarLauncher.launch("image/*") }, enabled = !uploadingAvatar) {
                Text(if (uploadingAvatar) "上传中..." else "更换头像")
            }

            Spacer(Modifier.height(8.dp))
            Text(session.username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(userInfo?.email ?: "", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)

            Spacer(Modifier.height(24.dp))
            if (loading) {
                CircularProgressIndicator()
            } else {
                val quota = userInfo?.quota ?: session.quota
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("存储空间", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = progressOf(usedSize, quota),
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("${humanSize(usedSize)} / ${humanSize(quota)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            errorMsg?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    session.logout()
                    onLoggedOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("退出登录") }
        }
    }
}
