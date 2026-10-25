package cn.xing.fileclouddisk.ui.screens

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.*
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.humanSize
import kotlinx.coroutines.launch

private const val TAG = "FileDetailScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    session: SessionViewModel,
    fileId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    var file by remember { mutableStateOf<FileItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var actionMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        errorMsg = null
        scope.launch {
            try {
                val api = ApiClient.getService(session.serverUrl)
                val resp = api.getFileDetail(fileId)
                if (resp.isSuccessful) {
                    file = resp.body()
                } else {
                    errorMsg = resp.errorMessageOrDefault("获取文件详情失败")
                }
            } catch (e: Exception) {
                AppLogger.logError(TAG, e, "获取文件详情失败: fileId=$fileId")
                errorMsg = "网络错误：${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(fileId) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件详情", modifier = Modifier.semantics { heading() }) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMsg != null -> Text(
                    errorMsg ?: "",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
                file != null -> {
                    val f = file!!
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text(f.filename, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("所有者", f.ownerUsername)
                        InfoRow("大小", humanSize(f.size))
                        InfoRow("类型", f.mimeType)
                        InfoRow("可见性", if (f.isPublic) "公开" else "私有")
                        InfoRow("创建时间", f.createdAt)
                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(f.url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("在浏览器中打开 / 下载") }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("link", f.url))
                                actionMsg = "链接已复制"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("复制直链") }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val api = ApiClient.getService(session.serverUrl)
                                        val resp = api.createShareBundle(
                                            ShareBundleCreateRequest(
                                                fileIds = listOf(f.id),
                                                username = session.username
                                            )
                                        )
                                        if (resp.isSuccessful) {
                                            val token = resp.body()?.bundleToken ?: ""
                                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                                            clipboard.setPrimaryClip(ClipData.newPlainText("share", token))
                                            actionMsg = "分享口令已复制：$token"
                                        } else {
                                            actionMsg = resp.errorMessageOrDefault("创建分享失败")
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.logError(TAG, e, "生成分享口令失败: fileId=${f.id}")
                                        actionMsg = "网络错误：${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("生成分享口令") }

                        
                        val canManage = session.isAdmin || f.ownerUsername == session.username
                        if (canManage) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val api = ApiClient.getService(session.serverUrl)
                                            val resp = api.patchFile(
                                                f.id,
                                                PatchFileRequest(PatchFileData(isPublic = !f.isPublic))
                                            )
                                            if (resp.isSuccessful) {
                                                load()
                                                actionMsg = if (!f.isPublic) "已设为公开" else "已设为私有"
                                            } else {
                                                actionMsg = resp.errorMessageOrDefault("操作失败")
                                            }
                                        } catch (e: Exception) {
                                            AppLogger.logError(TAG, e, "修改文件可见性失败: fileId=${f.id}")
                                            actionMsg = "网络错误：${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (f.isPublic) "设为私有" else "设为公开") }
                        }

                        
                        if (session.isAdmin) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("删除文件") }
                        }

                        actionMsg?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除后文件将移入回收站流程，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try {
                            val api = ApiClient.getService(session.serverUrl)
                            val resp = api.deleteFile(fileId, session.username)
                            if (resp.isSuccessful) {
                                onDeleted()
                            } else {
                                actionMsg = resp.errorMessageOrDefault("删除失败")
                            }
                        } catch (e: Exception) {
                            AppLogger.logError(TAG, e, "删除文件失败: fileId=$fileId")
                            actionMsg = "网络错误：${e.message}"
                        }
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.outline)
        Text(value)
    }
}
