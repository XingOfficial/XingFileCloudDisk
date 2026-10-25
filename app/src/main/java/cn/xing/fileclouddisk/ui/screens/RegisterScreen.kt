package cn.xing.fileclouddisk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.ApiClient
import cn.xing.fileclouddisk.network.RegisterRequest
import cn.xing.fileclouddisk.network.SendCodeRequest
import cn.xing.fileclouddisk.network.errorMessageOrDefault
import cn.xing.fileclouddisk.util.AppLogger
import kotlinx.coroutines.launch

private const val TAG = "RegisterScreen"

@Composable
fun RegisterScreen(
    session: SessionViewModel,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sendingCode by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("创建账号", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("用户名（3-20位字母数字下划线）") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("密码（至少6位）") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("邮箱") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("邮箱验证码") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = !sendingCode && email.isNotBlank(),
                onClick = {
                    sendingCode = true
                    errorMsg = null
                    infoMsg = null
                    scope.launch {
                        try {
                            val api = ApiClient.getService(session.serverUrl)
                            val resp = api.sendCode(SendCodeRequest(email.trim(), "register"))
                            if (resp.isSuccessful) {
                                infoMsg = "验证码已发送，请查收邮箱"
                            } else {
                                errorMsg = resp.errorMessageOrDefault("发送失败")
                            }
                        } catch (e: Exception) {
                            AppLogger.logError(TAG, e, "发送验证码失败")
                            errorMsg = "网络错误：${e.message}"
                        } finally {
                            sendingCode = false
                        }
                    }
                }
            ) { Text(if (sendingCode) "发送中" else "获取验证码") }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = question, onValueChange = { question = it },
            label = { Text("密保问题") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = answer, onValueChange = { answer = it },
            label = { Text("密保答案") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        infoMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !loading,
            onClick = {
                loading = true
                errorMsg = null
                scope.launch {
                    try {
                        val api = ApiClient.getService(session.serverUrl)
                        val resp = api.register(
                            RegisterRequest(
                                username.trim(), password, email.trim(), code.trim(),
                                question.trim(), answer.trim()
                            )
                        )
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            onRegistered()
                        } else {
                            errorMsg = resp.errorMessageOrDefault("注册失败")
                        }
                    } catch (e: Exception) {
                        AppLogger.logError(TAG, e, "注册失败")
                        errorMsg = "网络错误：${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("注册")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回登录") }
    }
}
