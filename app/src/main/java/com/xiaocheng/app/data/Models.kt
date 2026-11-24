package com.xiaocheng.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
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
    /** 用于展示“今天”等统计的参考时间：优先截止时间。 */
    val keyTime: Long? get() = deadlineAt ?: startAt

    /**
     * 是否归入「已到期」。
     * 规则：开始和截止都过去才算过期；如果截止已过但活动还没开始，仍放在未到期，
     * 避免“报名截止了，但活动还没开始”被提前归档。
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        if (done) return true
        val s = startAt
        val d = deadlineAt
        return when {
            s != null && d != null -> s < now && d < now
            s != null -> s < now
            d != null -> d < now
            else -> false
        }
    }

    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean = isExpired(now)

    /** 今天要关注、且还没到期的日程。跨天日程只要今天仍在区间内也算。 */
    fun isActiveToday(now: Long = System.currentTimeMillis()): Boolean {
        if (isExpired(now)) return false
        val zone = ZoneId.systemDefault()
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val nextDay = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val s = startAt ?: deadlineAt ?: return false
        val e = deadlineAt ?: startAt ?: return false
        return s < nextDay && e >= dayStart
    }

    /** 未到期时取最近的未来时间；已到期时取最近的过去时间。 */
    fun nextTimePoint(now: Long): EventTimePoint? {
        val points = buildList {
            startAt?.let { add(EventTimePoint(it, false)) }
            deadlineAt?.let { add(EventTimePoint(it, true)) }
        }.filter { it.at >= now }
        return points.sortedWith(compareBy({ it.at }, { if (it.isDeadline) 0 else 1 })).firstOrNull()
    }

    fun lastTimePoint(now: Long): EventTimePoint? {
        val points = buildList {
            startAt?.let { add(EventTimePoint(it, false)) }
            deadlineAt?.let { add(EventTimePoint(it, true)) }
        }.filter { it.at < now }
        return points.sortedWith(
            compareByDescending<EventTimePoint> { it.at }.thenBy { if (it.isDeadline) 0 else 1 }
        ).firstOrNull()
    }

    fun sortTime(now: Long = System.currentTimeMillis()): Long? =
        if (!isExpired(now)) nextTimePoint(now)?.at else lastTimePoint(now)?.at

    fun sortIsDeadline(now: Long = System.currentTimeMillis()): Boolean =
        if (!isExpired(now)) nextTimePoint(now)?.isDeadline == true else lastTimePoint(now)?.isDeadline == true

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

data class EventTimePoint(val at: Long, val isDeadline: Boolean)

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