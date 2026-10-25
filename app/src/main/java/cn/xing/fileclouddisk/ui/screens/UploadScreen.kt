package cn.xing.fileclouddisk.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Base64OutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import cn.xing.fileclouddisk.network.ApiClient
import cn.xing.fileclouddisk.network.UploadRequest
import cn.xing.fileclouddisk.network.errorMessageOrDefault
import cn.xing.fileclouddisk.util.AppLogger
import cn.xing.fileclouddisk.util.humanSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val TAG = "UploadScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    session: SessionViewModel,
    onUploaded: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var pickedSize by remember { mutableStateOf(0L) }
    var uploading by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var maxFileSize by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    
    LaunchedEffect(Unit) {
        try {
            val api = ApiClient.getService(session.serverUrl)
            val resp = api.getAppConfig()
            if (resp.isSuccessful) {
                maxFileSize = resp.body()?.maxFileSize
            }
        } catch (e: Exception) {
            
            AppLogger.logWarn(TAG, "获取服务端文件大小上限失败", e)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            errorMsg = null
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                if (it.moveToFirst()) {
                    pickedName = if (nameIdx >= 0) it.getString(nameIdx) ?: "file" else "file"
                    pickedSize = if (sizeIdx >= 0) it.getLong(sizeIdx) else 0L
                }
            }
            val limit = maxFileSize
            if (limit != null && limit > 0 && pickedSize > limit) {
                errorMsg = "文件过大，服务器限制单个文件最大 ${humanSize(limit)}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("上传文件", modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("取消") }
                }
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
            OutlinedButton(onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("选择文件")
            }
            Spacer(Modifier.height(16.dp))

            if (pickedUri != null) {
                Text(pickedName, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(humanSize(pickedSize), fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            } else {
                Text("尚未选择文件", color = MaterialTheme.colorScheme.outline)
            }

            errorMsg?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            val overLimit = maxFileSize?.let { it > 0 && pickedSize > it } ?: false
            Button(
                enabled = pickedUri != null && !uploading && !overLimit,
                onClick = {
                    val uri = pickedUri ?: return@Button
                    uploading = true
                    errorMsg = null
                    progressText = "正在处理文件..."
                    scope.launch {
                        try {
                            
                            
                            val (base64Content, mime) = withContext(Dispatchers.IO) {
                                val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                                val output = ByteArrayOutputStream()
                                Base64OutputStream(output, Base64.NO_WRAP).use { b64Stream ->
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        input.copyTo(b64Stream, bufferSize = 64 * 1024)
                                    } ?: throw java.io.IOException("无法读取所选文件")
                                }
                                output.toString("US-ASCII") to type
                            }
                            progressText = "正在上传..."
                            val api = ApiClient.getService(session.serverUrl)
                            val resp = api.upload(
                                UploadRequest(
                                    ownerUsername = session.username,
                                    filename = pickedName.ifBlank { "file" },
                                    content = base64Content,
                                    mimeType = mime
                                )
                            )
                            if (resp.isSuccessful) {
                                onUploaded()
                            } else {
                                errorMsg = resp.errorMessageOrDefault("上传失败")
                            }
                        } catch (oom: OutOfMemoryError) {
                            
                            
                            AppLogger.logError(TAG, oom, "上传文件内存不足: name=$pickedName, size=$pickedSize")
                            errorMsg = "文件过大，手机内存不足，无法上传，请换一个更小的文件试试"
                        } catch (e: Exception) {
                            AppLogger.logError(TAG, e, "上传文件失败: name=$pickedName, size=$pickedSize")
                            errorMsg = "上传出错：${e.message}"
                        } finally {
                            uploading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(progressText)
                } else {
                    Text("开始上传")
                }
            }
        }
    }
}
