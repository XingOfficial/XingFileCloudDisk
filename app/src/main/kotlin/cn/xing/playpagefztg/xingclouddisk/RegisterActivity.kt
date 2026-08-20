package cn.xing.playpagefztg.xingclouddisk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    private lateinit var inputUsername: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputSecurityQuestion: EditText
    private lateinit var inputSecurityAnswer: EditText
    private lateinit var btnPasswordRegister: Button
    private lateinit var authMsg: TextView
    private lateinit var goLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        inputUsername = findViewById(R.id.inputUsername)
        inputPassword = findViewById(R.id.inputPassword)
        inputSecurityQuestion = findViewById(R.id.inputSecurityQuestion)
        inputSecurityAnswer = findViewById(R.id.inputSecurityAnswer)
        btnPasswordRegister = findViewById(R.id.btnPasswordRegister)
        authMsg = findViewById(R.id.authMsg)
        goLogin = findViewById(R.id.goLogin)

        btnPasswordRegister.setOnClickListener { attemptRegister() }
        goLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptRegister() {
        val username = inputUsername.text.toString().trim()
        val password = inputPassword.text.toString().trim()
        val securityQuestion = inputSecurityQuestion.text.toString().trim()
        val securityAnswer = inputSecurityAnswer.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || securityQuestion.isEmpty() || securityAnswer.isEmpty()) {
            authMsg.text = "请填写所有字段"
            return
        }
        if (password.length < 6) {
            authMsg.text = "密码至少6位"
            return
        }

        setLoading(true)
        try {
            val body = JSONObject()
            body.put("username", username)
            body.put("password", password)
            body.put("securityQuestion", securityQuestion)
            body.put("securityAnswer", securityAnswer)

            ApiClient.request("/register", "POST", body) { result, err ->
                runOnUiThread {
                    setLoading(false)
                    if (err != null) {
                        authMsg.text = err
                        return@runOnUiThread
                    }
                    if (result != null && result.optBoolean("success", false)) {
                        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        authMsg.text = result?.optString("error", "注册失败") ?: "注册失败"
                    }
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            authMsg.text = "请求错误"
        }
    }

    private fun setLoading(loading: Boolean) {
        btnPasswordRegister.isEnabled = !loading
    }
}
