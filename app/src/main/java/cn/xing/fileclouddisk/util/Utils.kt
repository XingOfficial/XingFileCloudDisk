package cn.xing.fileclouddisk.util

import kotlin.math.min

fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    i = min(i, units.size - 1)
    return "%.2f %s".format(value, units[i])
}

fun progressOf(used: Long, total: Long): Float {
    if (total <= 0) return 0f
    return (used.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

fun relativeTimeText(millis: Long): String {
    if (millis <= 0) return "未知时间"
    val diff = System.currentTimeMillis() - millis
    if (diff < 0) return "刚刚"
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        else -> "${days}天前"
    }
}
