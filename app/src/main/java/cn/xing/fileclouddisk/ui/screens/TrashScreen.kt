package cn.xing.fileclouddisk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cn.xing.fileclouddisk.network.ApiClient
import cn.xing.fileclouddisk.network.TrashItem
import cn.xing.fileclouddisk.network.errorMessageOrDefault
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.humanSize
import kotlinx.coroutines.launch

private const val TAG = "TrashScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    session: SessionViewModel
) {
    var items by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        scope.launch {
            try {
                val api = ApiClient.getService(session.serverUrl)
                val resp = api.getTrash(session.username)
                if (resp.isSuccessful) {
                    val raw = resp.body()?.items ?: emptyList()
                    val deduped = raw.distinctBy { it.id }
                    if (deduped.size != raw.size) {
                        AppLogger.logWarn(TAG, "回收站列表存在重复id，已自动去重: ${raw.size} -> ${deduped.size}")
                    }
                    items = deduped
                } else {
                    errorMsg = resp.errorMessageOrDefault("获取回收站失败")
                }
            } catch (e: Exception) {
                AppLogger.logError(TAG, e, "获取回收站失败")
                errorMsg = "网络错误：${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站", modifier = Modifier.semantics { heading() }) },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val api = ApiClient.getService(session.serverUrl)
                                api.clearTrash(session.username)
                                load()
                            } catch (e: Exception) {
                                AppLogger.logError(TAG, e, "清空过期回收站失败")
                                errorMsg = "网络错误：${e.message}"
                            }
                        }
                    }) { Text("清空过期") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                items.isEmpty() -> Text("回收站为空", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.filename, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${humanSize(item.size)} · 剩余${item.daysLeft}天",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        val api = ApiClient.getService(session.serverUrl)
                                        api.restoreTrash(item.id, session.username)
                                        load()
                                    } catch (e: Exception) {
                                        AppLogger.logError(TAG, e, "恢复文件失败: id=${item.id}")
                                        errorMsg = "网络错误：${e.message}"
                                    }
                                }
                            }) { Text("恢复") }
                        }
                        Divider()
                    }
                }
            }
            errorMsg?.let {
                Text(
                    it,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
