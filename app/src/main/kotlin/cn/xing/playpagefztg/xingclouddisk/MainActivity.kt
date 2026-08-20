package cn.xing.playpagefztg.xingclouddisk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** 入口页：不显示实际内容，只根据登录状态决定跳去哪个页面。 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = Session.getUser(this)
        val next = Intent(this, if (user != null) DashboardActivity::class.java else LoginActivity::class.java)
        startActivity(next)
        finish()
    }
}
