package com.xiaocheng.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 一条校园日程。时间统一存为 epoch millis，null 表示未识别。 */
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var startAt: Long? = null,
    var deadlineAt: Long? = null,
    var location: String = "",
    var link: String = "",
    var category: String = "其他",
    var note: String = "",
    var source: String = "手动",
    var done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
) {
    /** 用于排序、判断是否到期的时间：优先截止时间。 */
    val keyTime: Long? get() = deadlineAt ?: startAt

    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean {
        if (done) return true
        val t = keyTime ?: return false
        return t < now
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("startAt", startAt ?: JSONObject.NULL)
        put("deadlineAt", deadlineAt ?: JSONObject.NULL)
        put("location", location)
        put("link", link)
        put("category", category)
        put("note", note)
        put("source", source)
        put("done", done)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(o: JSONObject): ScheduleEvent = ScheduleEvent(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = o.optString("title"),
            startAt = o.optLongOrNull("startAt"),
            deadlineAt = o.optLongOrNull("deadlineAt"),
            location = o.optString("location"),
            link = o.optString("link"),
            category = o.optString("category", "其他").ifBlank { "其他" },
            note = o.optString("note"),
            source = o.optString("source", "手动"),
            done = o.optBoolean("done", false),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
        )
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    val v = optLong(key, Long.MIN_VALUE)
    return if (v == Long.MIN_VALUE) null else v
}

/** AI / 规则抽取出来的候选事件，还没落库。 */
data class ExtractedEvent(
    val title: String,
    val startAt: Long?,
    val deadlineAt: Long?,
    val location: String = "",
    val link: String = "",
    val category: String = "其他",
    val note: String = "",
) {
    fun toScheduleEvent(source: String) = ScheduleEvent(
        title = title,
        startAt = startAt,
        deadlineAt = deadlineAt,
        location = location,
        link = link,
        category = category,
        note = note,
        source = source,
    )
}

data class ExtractionResult(
    val events: List<ExtractedEvent>,
    val engine: String,
    val raw: String = "",
    val warning: String? = null,
)

data class NoticeItem(
    val title: String,
    val url: String,
    val dateHint: String = "",
)

data class UpdateResult(
    val items: List<NoticeItem>,
    val firstRun: Boolean,
    val checkedAt: Long,
)
