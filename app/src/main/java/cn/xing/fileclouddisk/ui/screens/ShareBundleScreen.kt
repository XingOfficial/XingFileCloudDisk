package cn.xing.fileclouddisk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.*
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.humanSize
import kotlinx.coroutines.launch

private const val TAG = "ShareBundleScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBundleScreen(
    session: SessionViewModel,
    token: String,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var needPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val api = ApiClient.getService(session.serverUrl)
                val resp = api.getShareBundle(token)
                if (resp.isSuccessful) {
                    val raw = resp.body()?.items ?: emptyList()
                    val deduped = raw.distinctBy { it.id }
                    if (deduped.size != raw.size) {
                        AppLogger.logWarn(TAG, "分享文件列表存在重复id，已自动去重: ${raw.size} -> ${deduped.size}")
                    }
                    items = deduped
                } else {
                    val err = resp.errorMessageOrDefault("获取分享失败")
                    if (resp.code() == 403) {
                        needPassword = true
                        errorMsg = "该分享需要密码"
                    } else {
                        errorMsg = err
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError(TAG, e, "获取分享内容失败: token=$token")
                errorMsg = "网络错误：${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun verifyAndLoad() {
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val api = ApiClient.getService(session.serverUrl)
                val resp = api.verifySharePassword(ShareVerifyRequest(token, password))
                if (resp.isSuccessful) {
                    val raw = resp.body()?.items ?: emptyList()
                    val deduped = raw.distinctBy { it.id }
                    if (deduped.size != raw.size) {
                        AppLogger.logWarn(TAG, "分享文件列表存在重复id，已自动去重: ${raw.size} -> ${deduped.size}")
                    }
                    items = deduped
                    needPassword = false
                } else {
                    errorMsg = resp.errorMessageOrDefault("密码错误")
                }
            } catch (e: Exception) {
                AppLogger.logError(TAG, e, "校验分享密码失败: token=$token")
                errorMsg = "网络错误：${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(token) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分享包", modifier = Modifier.semantics { heading() }) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                needPassword -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("该分享需要密码", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("输入密码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { verifyAndLoad() }, modifier = Modifier.fillMaxWidth()) {
                            Text("确认")
                        }
                    }
                }
                errorMsg != null && items.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { load() }) { Text("重试") }
                    }
                }
                items.isEmpty() -> Text("分享包为空或已过期", modifier = Modifier.align(Alignment.Center))
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items, key = { it.id }) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (file.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (file.isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.filename, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(
                                        "${file.ownerUsername} · ${humanSize(file.size)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
