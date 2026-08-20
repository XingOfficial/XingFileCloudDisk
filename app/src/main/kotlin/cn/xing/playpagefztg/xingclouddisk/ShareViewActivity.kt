package cn.xing.playpagefztg.xingclouddisk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class ShareViewActivity : AppCompatActivity() {
    private lateinit var inputToken: EditText
    private lateinit var btnLoadShare: Button
    private lateinit var resultCard: LinearLayout
    private lateinit var previewImage: ImageView
    private lateinit var previewIcon: TextView
    private lateinit var btnPlayAudio: Button
    private lateinit var filenameLabel: TextView
    private lateinit var metaLabel: TextView
    private lateinit var btnDownload: Button
    private lateinit var statusMsg: TextView

    private var sharedFile: Models.FileRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_view)

        inputToken = findViewById(R.id.inputToken)
        btnLoadShare = findViewById(R.id.btnLoadShare)
        resultCard = findViewById(R.id.resultCard)
        previewImage = findViewById(R.id.previewImage)
        previewIcon = findViewById(R.id.previewIcon)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        filenameLabel = findViewById(R.id.filename)
        metaLabel = findViewById(R.id.meta)
        btnDownload = findViewById(R.id.btnDownload)
        statusMsg = findViewById(R.id.statusMsg)

        btnLoadShare.setOnClickListener {
            val token = extractToken(inputToken.text.toString().trim())
            if (token.isEmpty()) {
                statusMsg.text = "请输入分享 token 或链接"
                return@setOnClickListener
            }
            loadSharedFile(token)
        }

        btnDownload.setOnClickListener {
            val full = sharedFile ?: return@setOnClickListener
            try {
                FileStorageUtils.saveToDownloads(this, full.filename, full.mimeType, full.content ?: "")
                Toast.makeText(this, "已保存到下载目录", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show()
            }
        }

        btnPlayAudio.setOnClickListener {
            val full = sharedFile ?: return@setOnClickListener
            try {
                val uri = FileStorageUtils.writeToCacheAndGetUri(this, full.filename, full.content ?: "")
                val i = Intent(Intent.ACTION_VIEW)
                i.setDataAndType(uri, full.mimeType)
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(i, "打开文件"))
            } catch (e: Exception) {
                Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show()
            }
        }

        val intent = intent
        val data = intent.data
        if (Intent.ACTION_VIEW == intent.action && data != null) {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrEmpty()) {
                inputToken.setText(token)
                loadSharedFile(token)
            }
        }
    }

    private fun extractToken(raw: String): String {
        if (raw.isEmpty()) return raw
        val idx = raw.indexOf("token=")
        if (idx >= 0) {
            var token = raw.substring(idx + "token=".length)
            val amp = token.indexOf('&')
            if (amp >= 0) token = token.substring(0, amp)
            return token
        }
        return raw
    }

    private fun loadSharedFile(token: String) {
        statusMsg.text = "加载中…"
        resultCard.visibility = View.GONE
        val url = "/share?token=$token"
        ApiClient.request(url, "GET", null) { result, err ->
            runOnUiThread {
                if (err != null) {
                    showError("加载失败: $err")
                    return@runOnUiThread
                }
                try {
                    val items: JSONArray = result!!.getJSONArray("items")
                    if (items.length() == 0) {
                        showError("分享不存在")
                        return@runOnUiThread
                    }
                    val item = items.getJSONObject(0)
                    val mimeType = item.optString("mimeType", "application/octet-stream")
                    val f = Models.FileRecord(
                        id = item.getString("id"),
                        ownerUsername = item.optString("ownerUsername", ""),
                        filename = item.getString("filename"),
                        mimeType = mimeType,
                        category = Models.categoryOf(mimeType),
                        size = item.getLong("size"),
                        content = item.optString("content", null),
                        isPublic = item.optBoolean("isPublic", true),
                        shareToken = token
                    )
                    sharedFile = f
                    showResult(f)
                } catch (e: Exception) {
                    showError("解析失败")
                }
            }
        }
    }

    private fun showResult(f: Models.FileRecord) {
        statusMsg.text = ""
        resultCard.visibility = View.VISIBLE
        filenameLabel.text = f.filename
        metaLabel.text = "大小: " + Models.formatSize(f.size) + " · 上传者: " + f.ownerUsername

        previewImage.visibility = View.GONE
        previewIcon.visibility = View.GONE
        btnPlayAudio.visibility = View.GONE

        when (f.category) {
            "image" -> {
                try {
                    val bytes = Base64.decode(f.content, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        previewImage.setImageBitmap(bitmap)
                        previewImage.visibility = View.VISIBLE
                    } else {
                        previewIcon.text = "图片"
                        previewIcon.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    previewIcon.text = "图片"
                    previewIcon.visibility = View.VISIBLE
                }
            }
            "video" -> {
                previewIcon.text = "视频"
                previewIcon.visibility = View.VISIBLE
                btnPlayAudio.text = "播放视频"
                btnPlayAudio.visibility = View.VISIBLE
            }
            "audio" -> {
                previewIcon.text = "音频"
                previewIcon.visibility = View.VISIBLE
                btnPlayAudio.text = "播放音频"
                btnPlayAudio.visibility = View.VISIBLE
            }
            else -> {
                previewIcon.text = "文件"
                previewIcon.visibility = View.VISIBLE
            }
        }
    }

    private fun showError(msg: String) {
        statusMsg.text = msg
        resultCard.visibility = View.GONE
    }
}
