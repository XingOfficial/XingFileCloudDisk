package cn.xing.playpagefztg.xingclouddisk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URLEncoder

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var stepUsername: LinearLayout
    private lateinit var stepReset: LinearLayout

    private lateinit var inputUsername: EditText
    private lateinit var btnNext: Button

    private lateinit var securityQuestionLabel: TextView
    private lateinit var inputSecurityAnswer: EditText
    private lateinit var inputNewPassword: EditText
    private lateinit var btnReset: Button

    private lateinit var statusMsg: TextView
    private lateinit var goLogin: TextView

    private var verifiedUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        stepUsername = findViewById(R.id.stepUsername)
        stepReset = findViewById(R.id.stepReset)

        inputUsername = findViewById(R.id.inputUsername)
        btnNext = findViewById(R.id.btnNext)

        securityQuestionLabel = findViewById(R.id.securityQuestionLabel)
        inputSecurityAnswer = findViewById(R.id.inputSecurityAnswer)
        inputNewPassword = findViewById(R.id.inputNewPassword)
        btnReset = findViewById(R.id.btnReset)

        statusMsg = findViewById(R.id.statusMsg)
        goLogin = findViewById(R.id.goLogin)

        btnNext.setOnClickListener { fetchSecurityQuestion() }
        btnReset.setOnClickListener { attemptReset() }
        goLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchSecurityQuestion() {
        val username = inputUsername.text.toString().trim()
        if (username.isEmpty()) {
            statusMsg.text = "请填写用户名"
            return
        }
        statusMsg.text = ""
        btnNext.isEnabled = false
        val encoded = URLEncoder.encode(username, "UTF-8")
        ApiClient.request("/security-question?username=$encoded", "GET", null) { result, err ->
            runOnUiThread {
                btnNext.isEnabled = true
                if (err != null) {
                    statusMsg.text = err
                    return@runOnUiThread
                }
                val question = result?.optString("securityQuestion", "") ?: ""
                if (question.isEmpty()) {
                    statusMsg.text = "获取密保问题失败"
                    return@runOnUiThread
                }
                verifiedUsername = username
                securityQuestionLabel.text = "密保问题：$question"
                stepUsername.visibility = View.GONE
                stepReset.visibility = View.VISIBLE
            }
        }
    }

    private fun attemptReset() {
        val username = verifiedUsername ?: return
        val answer = inputSecurityAnswer.text.toString().trim()
        val newPassword = inputNewPassword.text.toString().trim()

        if (answer.isEmpty() || newPassword.isEmpty()) {
            statusMsg.text = "请填写密保答案和新密码"
            return
        }
        if (newPassword.length < 6) {
            statusMsg.text = "新密码至少6位"
            return
        }

        btnReset.isEnabled = false
        try {
            val body = JSONObject()
            body.put("username", username)
            body.put("securityAnswer", answer)
            body.put("newPassword", newPassword)

            ApiClient.request("/reset-password", "POST", body) { result, err ->
                runOnUiThread {
                    btnReset.isEnabled = true
                    if (err != null) {
                        statusMsg.text = err
                        return@runOnUiThread
                    }
                    if (result != null && result.optBoolean("success", false)) {
                        Toast.makeText(this, "密码已重置，请重新登录", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        statusMsg.text = "重置失败"
                    }
                }
            }
        } catch (e: Exception) {
            btnReset.isEnabled = true
            statusMsg.text = "请求错误"
        }
    }
}
