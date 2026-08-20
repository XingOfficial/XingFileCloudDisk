package cn.xing.playpagefztg.xingclouddisk

object Models {

    /**
     * 文件记录，和网页版一致的 base64 上传模型：文件内容直接存进 content 字段（base64
     * 文本），不是外部直链。注意：读取记录列表接口对大的 text 字段（也就是 content）
     * 可能会做截断，只有读取单条记录接口才会返回完整内容 —— 涉及预览/下载/播放时，
     * 一定要先用 id 重新拉一次完整记录，不能直接用列表接口拿到的 content。
     */
    class FileRecord(
        val id: String,
        val ownerUsername: String,
        val filename: String,
        val mimeType: String,
        val category: String,
        val size: Long,
        val content: String?, // base64，可能是列表接口截断过的，只用于占位判断
        val isPublic: Boolean,
        val shareToken: String?, // 可能为 null / 空字符串

        // 分享相关字段
        val sharePasswordHash: String? = null,
        val sharePasswordSalt: String? = null,
        val shareExpiresAt: Long = 0L
    ) {

        fun hasSharePassword(): Boolean {
            return !sharePasswordHash.isNullOrEmpty()
        }
    }

    /** 格式化文件大小；0 或缺失时显示"大小未知"。 */
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "大小未知"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        return String.format("%.2f MB", kb / 1024.0)
    }

    /** 从 MIME 类型猜分类，用于挑选文件选择器返回的文件应该按图片/音频/视频/其他处理。 */
    fun categoryOf(mimeType: String?): String {
        if (mimeType == null) return "other"
        if (mimeType.startsWith("image/")) return "image"
        if (mimeType.startsWith("audio/")) return "audio"
        if (mimeType.startsWith("video/")) return "video"
        return "other"
    }
}
