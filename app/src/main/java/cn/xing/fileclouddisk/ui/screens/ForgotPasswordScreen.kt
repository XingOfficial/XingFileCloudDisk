package cn.xing.fileclouddisk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.*
import cn.xing.fileclouddisk.util.AppLogger
import kotlinx.coroutines.launch

private const val TAG = "ForgotPasswordScreen"

private enum class ForgotMode { EMAIL, SECURITY }

@Composable
fun ForgotPasswordScreen(
    session: SessionViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(ForgotMode.EMAIL) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPasswordEmail by remember { mutableStateOf("") }
    var sendingCode by remember { mutableStateOf(false) }

    
    var username by remember { mutableStateOf("") }
    var question by remember { mutableStateOf<String?>(null) }
    var answer by remember { mutableStateOf("") }
    var newPasswordSecurity by remember { mutableStateOf("") }
    var loadingQuestion by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("找回密码", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        TabRow(selectedTabIndex = if (mode == ForgotMode.EMAIL) 0 else 1) {
            Tab(selected = mode == ForgotMode.EMAIL, onClick = { mode = ForgotMode.EMAIL; errorMsg = null }, text = { Text("邮箱验证码") })
            Tab(selected = mode == ForgotMode.SECURITY, onClick = { mode = ForgotMode.SECURITY; errorMsg = null }, text = { Text("密保问题") })
        }
        Spacer(Modifier.height(16.dp))

        if (mode == ForgotMode.EMAIL) {
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("邮箱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row {
                OutlinedTextField(
                    value = code, onValueChange = { code = it }, label = { Text("验证码") },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = !sendingCode && email.isNotBlank(),
                    onClick = {
                        sendingCode = true; errorMsg = null; infoMsg = null
                        scope.launch {
                            try {
                                val api = ApiClient.getService(session.serverUrl)
                                val resp = api.sendCode(SendCodeRequest(email.trim(), "reset"))
                                infoMsg = if (resp.isSuccessful) "验证码已发送" else null
                                if (!resp.isSuccessful) errorMsg = resp.errorMessageOrDefault("发送失败")
                            } catch (e: Exception) {
                                AppLogger.logError(TAG, e, "发送重置验证码失败")
                                errorMsg = "网络错误：${e.message}"
                            } finally { sendingCode = false }
                        }
                    }
                ) { Text(if (sendingCode) "发送中" else "获取验证码") }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newPasswordEmail, onValueChange = { newPasswordEmail = it },
                label = { Text("新密码") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = username, onValueChange = { username = it; question = null },
                label = { Text("用户名") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                enabled = !loadingQuestion && username.isNotBlank(),
                onClick = {
                    loadingQuestion = true; errorMsg = null
                    scope.launch {
                        try {
                            val api = ApiClient.getService(session.serverUrl)
                            val resp = api.getSecurityQuestion(username.trim())
                            if (resp.isSuccessful) {
                                question = resp.body()?.securityQuestion ?: ""
                            } else {
                                errorMsg = resp.errorMessageOrDefault("获取密保问题失败")
                            }
                        } catch (e: Exception) {
                            AppLogger.logError(TAG, e, "获取密保问题失败")
                            errorMsg = "网络错误：${e.message}"
                        } finally { loadingQuestion = false }
                    }
                }
            ) { Text("获取密保问题") }

            question?.let {
                Spacer(Modifier.height(10.dp))
                Text("密保问题：$it", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = answer, onValueChange = { answer = it },
                    label = { Text("密保答案") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPasswordSecurity, onValueChange = { newPasswordSecurity = it },
                    label = { Text("新密码") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

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
                loading = true; errorMsg = null
                scope.launch {
                    try {
                        val api = ApiClient.getService(session.serverUrl)
                        val resp = if (mode == ForgotMode.EMAIL) {
                            api.resetPasswordEmail(ResetPasswordEmailRequest(email.trim(), code.trim(), newPasswordEmail))
                        } else {
                            api.resetPassword(ResetPasswordRequest(username.trim(), answer.trim(), newPasswordSecurity))
                        }
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            onDone()
                        } else {
                            errorMsg = resp.errorMessageOrDefault("重置失败")
                        }
                    } catch (e: Exception) {
                        AppLogger.logError(TAG, e, "重置密码失败")
                        errorMsg = "网络错误：${e.message}"
                    } finally { loading = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("重置密码")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回登录") }
    }
}
