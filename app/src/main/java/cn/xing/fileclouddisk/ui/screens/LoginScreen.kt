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
import cn.xing.fileclouddisk.network.LoginRequest
import cn.xing.fileclouddisk.network.errorMessageOrDefault
import cn.xing.fileclouddisk.util.AppLogger
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    session: SessionViewModel,
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    onGoForgot: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("欢迎回来", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(session.serverUrl, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMsg = null },
            label = { Text("用户名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMsg = null },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMsg = "请输入用户名和密码"
                    return@Button
                }
                loading = true
                errorMsg = null
                scope.launch {
                    try {
                        val api = ApiClient.getService(session.serverUrl)
                        val resp = api.login(LoginRequest(username.trim(), password))
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            val body = resp.body()!!
                            session.onLoginSuccess(body.username, body.quota, body.avatar, body.token)
                            onLoginSuccess()
                        } else {
                            errorMsg = resp.errorMessageOrDefault("登录失败")
                        }
                    } catch (e: Exception) {
                        AppLogger.logError(TAG, e, "登录请求失败")
                        errorMsg = "网络错误：${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("登录")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onGoRegister) { Text("注册账号") }
            TextButton(onClick = onGoForgot) { Text("忘记密码") }
        }
    }
}
