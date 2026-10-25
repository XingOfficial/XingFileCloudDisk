package cn.xing.fileclouddisk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xing.fileclouddisk.data.FileListCache
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.ApiClient
import cn.xing.fileclouddisk.network.FileItem
import cn.xing.fileclouddisk.network.errorMessageOrDefault
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.humanSize
import cn.xing.fileclouddisk.util.relativeTimeText
import kotlinx.coroutines.launch

private const val TAG = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: SessionViewModel,
    onOpenFile: (FileItem) -> Unit,
    onUpload: () -> Unit
) {
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var isOfflineCache by remember { mutableStateOf(false) }
    var cacheSavedAt by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fileListCache = remember { FileListCache(context) }

    fun loadCacheFallback(): Boolean {
        val cached = fileListCache.load(session.username) ?: return false
        if (cached.items.isEmpty()) return false
        files = cached.items
        isOfflineCache = true
        cacheSavedAt = cached.savedAtMillis
        return true
    }

    fun refresh() {
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val api = ApiClient.getService(session.serverUrl)
                val resp = api.getFiles(session.username)
                if (resp.isSuccessful) {
                    val raw = resp.body()?.items ?: emptyList()
                    val deduped = raw.distinctBy { it.id }
                    if (deduped.size != raw.size) {
                        AppLogger.logWarn(TAG, "文件列表存在重复id，已自动去重: ${raw.size} -> ${deduped.size}")
                    }
                    files = deduped
                    isOfflineCache = false
                    fileListCache.save(session.username, deduped)
                } else {
                    errorMsg = resp.errorMessageOrDefault("加载文件列表失败")
                    loadCacheFallback()
                }
            } catch (e: Exception) {
                AppLogger.logError(TAG, e, "加载文件列表失败")
                errorMsg = "网络错误：${e.message}"
                loadCacheFallback()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("直链云盘") },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onUpload) {
                Icon(Icons.Default.Add, contentDescription = "上传")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading && files.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMsg != null && files.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { refresh() }) { Text("重试") }
                    }
                }
                files.isEmpty() -> {
                    Text("暂无文件", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isOfflineCache) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "服务器连接失败，当前显示${relativeTimeText(cacheSavedAt)}的缓存数据",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { refresh() }) { Text("重试") }
                                }
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(files, key = { it.id }) { file ->
                                FileRow(file = file, onClick = { onOpenFile(file) }, currentUsername = session.username)
                                Divider()
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: FileItem, onClick: () -> Unit, currentUsername: String = "") {
    val isMine = file.ownerUsername == currentUsername
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${file.filename}, ${if (file.isPublic) "公开" else "私有"}, 大小 ${humanSize(file.size)}, 上传者 ${file.ownerUsername}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isPublic) Icons.Default.Public else Icons.Default.Lock,
            contentDescription = if (file.isPublic) "公开文件" else "私有文件",
            tint = if (file.isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                file.filename,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${file.ownerUsername}${if (isMine) " (我)" else ""} · ${humanSize(file.size)}",
                fontSize = 12.sp,
                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}
