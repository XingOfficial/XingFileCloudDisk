package cn.xing.playpagefztg.xingclouddisk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var inputUsername: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnPasswordLogin: Button
    private lateinit var authMsg: TextView
    private lateinit var goRegister: TextView
    private lateinit var goForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        inputUsername = findViewById(R.id.inputUsername)
        inputPassword = findViewById(R.id.inputPassword)
        btnPasswordLogin = findViewById(R.id.btnPasswordLogin)
        authMsg = findViewById(R.id.authMsg)
        goRegister = findViewById(R.id.goRegister)
        goForgotPassword = findViewById(R.id.goForgotPassword)

        btnPasswordLogin.setOnClickListener { attemptLogin() }
        goRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        goForgotPassword.setOnClickListener { startActivity(Intent(this, ForgotPasswordActivity::class.java)) }
    }

    private fun attemptLogin() {
        val username = inputUsername.text.toString().trim()
        val password = inputPassword.text.toString().trim()
        if (username.isEmpty() || password.isEmpty()) {
            authMsg.text = "请填写用户名和密码"
            return
        }
        setLoading(true)
        try {
            val body = JSONObject()
            body.put("username", username)
            body.put("password", password)

            ApiClient.request("/login", "POST", body) { result, err ->
                runOnUiThread {
                    setLoading(false)
                    if (err != null) {
                        authMsg.text = err
                        return@runOnUiThread
                    }
                    if (result != null && result.optBoolean("success", false)) {
                        Session.setUser(this, username)
                        Session.setQuota(this, result.optLong("quota", 0))
                        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else {
                        authMsg.text = result?.optString("error", "登录失败") ?: "登录失败"
                    }
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            authMsg.text = "请求错误"
        }
    }

    private fun setLoading(loading: Boolean) {
        btnPasswordLogin.isEnabled = !loading
    }
}
