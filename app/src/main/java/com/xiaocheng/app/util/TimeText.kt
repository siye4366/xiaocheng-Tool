package com.xiaocheng.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()
private val WEEK = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun dayOfWeekCn(ms: Long): String {
    val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    return WEEK[d.dayOfWeek.value - 1]
}

fun toLocal(ms: Long): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()

fun startOfDay(ms: Long): Long =
    Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

/** 11月25日 周二 19:00 */
fun formatFull(ms: Long): String {
    val t = toLocal(ms)
    return String.format("%d月%d日 %s %02d:%02d", t.monthValue, t.dayOfMonth, dayOfWeekCn(ms), t.hour, t.minute)
}

/** 11月25日 19:00 */
fun formatShort(ms: Long): String {
    val t = toLocal(ms)
    return String.format("%d月%d日 %02d:%02d", t.monthValue, t.dayOfMonth, t.hour, t.minute)
}

fun formatTime(ms: Long): String {
    val t = toLocal(ms)
    return String.format("%02d:%02d", t.hour, t.minute)
}

fun formatDateOnly(ms: Long): String {
    val t = toLocal(ms)
    return String.format("%d月%d日", t.monthValue, t.dayOfMonth)
}

/** 今天 / 明天 / 昨天 / 11月25日 */
fun dayLabel(ms: Long, now: Long = System.currentTimeMillis()): String {
    val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when (d) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.plusDays(2) -> "后天"
        today.minusDays(1) -> "昨天"
        else -> String.format("%d月%d日", d.monthValue, d.dayOfMonth)
    }
}

/** 距离 keyTime 的自然语言倒计时。 */
fun countdownText(ms: Long?, now: Long = System.currentTimeMillis()): String {
    if (ms == null) return "待补充时间"
    val diff = ms - now
    if (diff < 0) {
        val days = (-diff) / 86_400_000L
        return if (days >= 1) "已过期 " + days + " 天" else "已过期"
    }
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 60 -> Math.max(1, minutes).toString() + " 分钟后"
        hours < 24 -> hours.toString() + " 小时后"
        days == 1L -> "明天到期"
        days < 7 -> days.toString() + " 天后"
        else -> (days / 7).toString() + " 周后"
    }
}

/** 距离开始时间的相对描述，例如“1 天后开始”“3 小时后开始”“已开始”。 */
fun startCountdownText(ms: Long?, now: Long = System.currentTimeMillis()): String {
    if (ms == null) return "待补充时间"
    val diff = ms - now
    if (diff < 0) return "已开始"
    return futureRelative(diff, "开始")
}

/** 距离截止时间的相对描述，例如“2 天后截止”“已截止 1 天”。 */
fun deadlineCountdownText(ms: Long?, now: Long = System.currentTimeMillis()): String {
    if (ms == null) return "待补充时间"
    val diff = ms - now
    if (diff < 0) {
        val days = (-diff) / 86_400_000L
        return if (days >= 1) "已截止 " + days + " 天" else "已截止"
    }
    return futureRelative(diff, "截止")
}

private fun futureRelative(diff: Long, suffix: String): String {
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 1 -> "即将" + suffix
        minutes < 60 -> minutes.toString() + " 分钟后" + suffix
        hours < 24 -> hours.toString() + " 小时后" + suffix
        days < 30 -> days.toString() + " 天后" + suffix
        days < 365 -> (days / 30).toString() + " 个月后" + suffix
        else -> (days / 365).toString() + " 年后" + suffix
    }
}

fun isToday(ms: Long, now: Long = System.currentTimeMillis()): Boolean {
    val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return d == today
}

fun greeting(now: LocalDateTime = LocalDateTime.now()): String = when (now.hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    in 18..22 -> "晚上好"
    else -> "夜深了"
}

fun todayLabel(now: LocalDateTime = LocalDateTime.now()): String {
    val d = now.toLocalDate()
    return String.format("%d月%d日 · %s", d.monthValue, d.dayOfMonth, WEEK[d.dayOfWeek.value - 1])
}

fun dateTimePreview(ms: Long?): String = if (ms == null) "未设置" else formatFull(ms)

fun combine(date: LocalDate, hour: Int, minute: Int): Long =
    date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

fun isoStamp(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(zone).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.CHINA))