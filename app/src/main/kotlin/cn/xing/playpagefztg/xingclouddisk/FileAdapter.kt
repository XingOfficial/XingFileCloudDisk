package cn.xing.playpagefztg.xingclouddisk

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(private val listener: Listener) : RecyclerView.Adapter<FileAdapter.VH>() {

    /** 完整记录到手之后（绕开列表接口可能的截断），用它把真正的预览内容画出来。 */
    fun interface PreviewTarget {
        fun onFullRecordReady(fullRecord: Models.FileRecord)
    }

    interface Listener {
        fun onDownload(item: Models.FileRecord)
        fun onShare(item: Models.FileRecord)
        fun onDelete(item: Models.FileRecord)
        fun onPlay(item: Models.FileRecord)

        /** 请求 Activity 用"读取单条记录"接口拉完整数据，拿到后回调 target。 */
        fun loadPreview(item: Models.FileRecord, target: PreviewTarget)
    }

    private val items = mutableListOf<Models.FileRecord>()

    fun submitList(newItems: List<Models.FileRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        // 用 id 标记这次绑定，异步预览回来时如果 ViewHolder 已经被复用去绑别的条目就跳过。
        holder.itemView.tag = item.id

        holder.previewImage.visibility = View.GONE
        holder.previewIcon.visibility = View.GONE
        holder.btnPlay.visibility = View.GONE

        when (item.category) {
            "image" -> {
                holder.previewIcon.text = "图片"
                holder.previewIcon.visibility = View.VISIBLE
            }
            "video" -> {
                holder.previewIcon.text = "视频"
                holder.previewIcon.visibility = View.VISIBLE
                holder.btnPlay.text = "播放视频"
                holder.btnPlay.visibility = View.VISIBLE
            }
            "audio" -> {
                holder.previewIcon.text = "音频"
                holder.previewIcon.visibility = View.VISIBLE
                holder.btnPlay.text = "播放音频"
                holder.btnPlay.visibility = View.VISIBLE
            }
            else -> {
                holder.previewIcon.text = "文件"
                holder.previewIcon.visibility = View.VISIBLE
            }
        }

        if (item.category == "image") {
            listener.loadPreview(item) { fullRecord ->
                if (item.id != holder.itemView.tag) return@loadPreview // 视图已被复用，丢弃
                try {
                    val bytes = Base64.decode(fullRecord.content, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        holder.previewImage.setImageBitmap(bitmap)
                        holder.previewImage.visibility = View.VISIBLE
                        holder.previewIcon.visibility = View.GONE
                    }
                } catch (ignored: Exception) {
                    // 解码失败就保留占位图标
                }
            }
        }

        holder.filename.text = item.filename
        holder.meta.text = Models.formatSize(item.size) + " · " + (if (item.isPublic) "已分享" else "仅自己可见")

        holder.btnDownload.setOnClickListener { listener.onDownload(item) }
        holder.btnShare.setOnClickListener { listener.onShare(item) }
        holder.btnDelete.setOnClickListener { listener.onDelete(item) }
        holder.btnPlay.setOnClickListener { listener.onPlay(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val previewImage: ImageView = itemView.findViewById(R.id.previewImage)
        val previewIcon: TextView = itemView.findViewById(R.id.previewIcon)
        val btnPlay: Button = itemView.findViewById(R.id.btnPlayAudio)
        val filename: TextView = itemView.findViewById(R.id.filename)
        val meta: TextView = itemView.findViewById(R.id.meta)
        val btnDownload: Button = itemView.findViewById(R.id.btnDownload)
        val btnShare: Button = itemView.findViewById(R.id.btnShare)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }
}
