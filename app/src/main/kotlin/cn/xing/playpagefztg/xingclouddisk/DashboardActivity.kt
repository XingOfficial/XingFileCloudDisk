package cn.xing.playpagefztg.xingclouddisk

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class DashboardActivity : AppCompatActivity() {
    private lateinit var greeting: TextView
    private lateinit var btnLogout: Button
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var maxSizeLabel: TextView
    private lateinit var btnPickFile: Button
    private lateinit var selectedFileLabel: TextView
    private lateinit var inputFilename: EditText
    private lateinit var btnUpload: Button
    private lateinit var uploadMsg: TextView
    private lateinit var emptyLabel: TextView
    private lateinit var fileListView: RecyclerView

    private lateinit var adapter: FileAdapter
    private val fileItems = mutableListOf<Models.FileRecord>()
    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        greeting = findViewById(R.id.greeting)
        btnLogout = findViewById(R.id.btnLogout)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        maxSizeLabel = findViewById(R.id.maxSizeLabel)
        btnPickFile = findViewById(R.id.btnPickFile)
        selectedFileLabel = findViewById(R.id.selectedFileLabel)
        inputFilename = findViewById(R.id.inputFilename)
        btnUpload = findViewById(R.id.btnUpload)
        uploadMsg = findViewById(R.id.uploadMsg)
        emptyLabel = findViewById(R.id.emptyLabel)
        fileListView = findViewById(R.id.fileList)

        fileListView.layoutManager = LinearLayoutManager(this)
        fileListView.isNestedScrollingEnabled = false
        adapter = FileAdapter(object : FileAdapter.Listener {
            override fun onDownload(item: Models.FileRecord) { downloadFile(item) }
            override fun onShare(item: Models.FileRecord) { shareFile(item) }
            override fun onDelete(item: Models.FileRecord) { deleteFile(item) }
            override fun onPlay(item: Models.FileRecord) { playFile(item) }
            override fun loadPreview(item: Models.FileRecord, target: FileAdapter.PreviewTarget) {
                fetchFullRecord(item) { full, err ->
                    if (err == null && full != null) {
                        runOnUiThread { target.onFullRecordReady(full) }
                    }
                }
            }
        })
        fileListView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadFiles() }
        btnPickFile.setOnClickListener { pickFile() }
        btnUpload.setOnClickListener { attemptUpload() }
        btnLogout.setOnClickListener {
            Session.clear(this@DashboardActivity)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val user = Session.getUser(this)
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        greeting.text = "用户: $user · 配额: " + Models.formatSize(Session.getQuota(this))
        maxSizeLabel.text = "单文件上限: " + Models.formatSize(Config.MAX_FILE_BYTES)
        loadFiles()
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                selectedUri = uri
                val name = getFileName(uri)
                selectedFileLabel.text = "已选择: $name"
                inputFilename.setText(name)
                uploadMsg.text = ""
            }
        }
    }

    private fun attemptUpload() {
        val uri = selectedUri
        if (uri == null) {
            uploadMsg.text = "请先选择文件"
            return
        }
        val filename = inputFilename.text.toString().trim().ifEmpty { getFileName(uri) }
        uploadFile(uri, filename)
    }

    private fun loadFiles() {
        setLoading(true)
        val username = Session.getUser(this)
        ApiClient.request("/files?username=$username", "GET", null) { result, err ->
            runOnUiThread {
                setLoading(false)
                if (err != null) {
                    Toast.makeText(this, "加载失败: $err", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                try {
                    val items: JSONArray = result!!.getJSONArray("items")
                    fileItems.clear()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val mimeType = item.optString("mimeType", "application/octet-stream")
                        val f = Models.FileRecord(
                            id = item.getString("id"),
                            ownerUsername = item.optString("ownerUsername", ""),
                            filename = item.getString("filename"),
                            mimeType = mimeType,
                            category = Models.categoryOf(mimeType),
                            size = item.getLong("size"),
                            content = null,
                            isPublic = item.optBoolean("isPublic", true),
                            shareToken = null
                        )
                        fileItems.add(f)
                    }
                    adapter.submitList(fileItems)
                    emptyLabel.visibility = if (fileItems.isEmpty()) View.VISIBLE else View.GONE
                    emptyLabel.text = "暂无文件"
                } catch (e: Exception) {
                    Toast.makeText(this, "解析失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadFile(uri: Uri, filename: String) {
        setLoading(true)
        try {
            val bytes = readUriBytes(uri)
            if (bytes == null) {
                uploadMsg.text = "读取文件失败"
                setLoading(false)
                return
            }
            if (bytes.size > Config.MAX_FILE_BYTES) {
                uploadMsg.text = "文件过大，最大" + Models.formatSize(Config.MAX_FILE_BYTES)
                setLoading(false)
                return
            }
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            val owner = Session.getUser(this)
            ApiClient.uploadFile(owner ?: "", filename, mimeType, base64) { _, err ->
                runOnUiThread {
                    setLoading(false)
                    if (err != null) {
                        uploadMsg.text = "上传失败: $err"
                    } else {
                        uploadMsg.text = "上传成功"
                        selectedUri = null
                        selectedFileLabel.text = "未选择文件"
                        inputFilename.setText("")
                        loadFiles()
                    }
                }
            }
        } catch (e: Exception) {
            setLoading(false)
            uploadMsg.text = "上传错误"
        }
    }

    @Throws(IOException::class)
    private fun readUriBytes(uri: Uri): ByteArray? {
        val isStream = contentResolver.openInputStream(uri) ?: return null
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var n: Int
        isStream.use { input ->
            while (input.read(buf).also { n = it } != -1) {
                baos.write(buf, 0, n)
            }
        }
        return baos.toByteArray()
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = it.getString(idx)
                }
            }
        }
        if (result == null) result = uri.lastPathSegment
        return result ?: "unknown"
    }

    private fun downloadFile(f: Models.FileRecord) {
        fetchFullRecord(f) { full, err ->
            runOnUiThread {
                if (err != null || full == null) {
                    Toast.makeText(this, "获取文件信息失败", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                try {
                    FileStorageUtils.saveToDownloads(this, full.filename, full.mimeType, full.content ?: "")
                    Toast.makeText(this, "已保存到下载目录", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playFile(f: Models.FileRecord) {
        fetchFullRecord(f) { full, err ->
            runOnUiThread {
                if (err != null || full == null) {
                    Toast.makeText(this, "获取文件信息失败", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                try {
                    val uri = FileStorageUtils.writeToCacheAndGetUri(this, full.filename, full.content ?: "")
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, full.mimeType)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(intent, "打开文件"))
                } catch (e: Exception) {
                    Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteFile(f: Models.FileRecord) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("删除 ${f.filename}?")
            .setPositiveButton("删除") { _, _ ->
                setLoading(true)
                val username = Session.getUser(this)
                ApiClient.request("/files/${f.id}?username=$username", "DELETE", null) { _, err ->
                    runOnUiThread {
                        setLoading(false)
                        if (err != null) {
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                            loadFiles()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun shareFile(f: Models.FileRecord) {
        // 直链链接：直接指向服务器 uploads 目录下的实际文件（和网页版"复制直链"一致），
        // 不再走 /api/share?token=... 那个只返回 JSON 详情的接口。
        val shareUrl = buildDirectUrl(f.filename)
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("分享链接", shareUrl))
        Toast.makeText(this, "直链已复制", Toast.LENGTH_SHORT).show()
    }

    private fun buildDirectUrl(filename: String): String {
        val encoded = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")
        return Config.API_HOST + "/uploads/" + encoded
    }

    private fun fetchFullRecord(item: Models.FileRecord, cb: (Models.FileRecord?, String?) -> Unit) {
        ApiClient.request("/files/${item.id}", "GET", null) { result, err ->
            if (err != null) {
                cb(null, err)
                return@request
            }
            try {
                val mimeType = result!!.optString("mimeType", "application/octet-stream")
                val f = Models.FileRecord(
                    id = result.getString("id"),
                    ownerUsername = result.optString("ownerUsername", ""),
                    filename = result.getString("filename"),
                    mimeType = mimeType,
                    category = Models.categoryOf(mimeType),
                    size = result.getLong("size"),
                    content = result.optString("content", null),
                    isPublic = result.optBoolean("isPublic", true),
                    shareToken = result.optString("shareToken", null)
                )
                cb(f, null)
            } catch (e: Exception) {
                cb(null, e.message)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        swipeRefresh.isRefreshing = loading
    }
} 