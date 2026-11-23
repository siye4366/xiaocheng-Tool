package com.xiaocheng.app.export

import android.content.Context
import com.xiaocheng.app.data.ScheduleEvent
import com.xiaocheng.app.util.formatFull
import com.xiaocheng.app.util.isoStamp
import java.io.File

/** 手写 iCalendar（RFC 5545）导出，避免额外依赖。时间使用本地浮动时间，导入手机日历后按本地时区显示。 */
object IcsExporter {

    fun build(events: List<ScheduleEvent>, now: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        fun line(s: String) { sb.append(s).append("\r\n") }

        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//XiaoCheng//Campus Schedule//CN")
        line("CALSCALE:GREGORIAN")
        line("METHOD:PUBLISH")
        line("X-WR-CALNAME:校程日程")
        line("X-WR-TIMEZONE:Asia/Shanghai")

        events.filter { !it.done }.forEach { ev ->
            val at = ev.startAt ?: ev.deadlineAt ?: return@forEach
            val end = at + 30 * 60 * 1000L
            line("BEGIN:VEVENT")
            line("UID:" + ev.id + "@xiaocheng.app")
            line("DTSTAMP:" + isoStamp(now))
            line("DTSTART:" + isoStamp(at))
            line("DTEND:" + isoStamp(end))
            line("SUMMARY:" + escape(summaryOf(ev)))
            line("LOCATION:" + escape(ev.location))
            line("CATEGORIES:" + escape(ev.category))
            line("DESCRIPTION:" + escape(descriptionOf(ev)))
            line("STATUS:CONFIRMED")
            line("BEGIN:VALARM")
            line("TRIGGER:-PT30M")
            line("ACTION:DISPLAY")
            line("DESCRIPTION:" + escape("校程提醒：" + ev.title))
            line("END:VALARM")
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
        return sb.toString()
    }

    private fun summaryOf(ev: ScheduleEvent): String =
        if (ev.startAt == null && ev.deadlineAt != null) "截止：" + ev.title else ev.title

    private fun descriptionOf(ev: ScheduleEvent): String {
        val parts = mutableListOf<String>()
        if (ev.startAt != null) parts.add("开始：" + formatFull(ev.startAt!!))
        if (ev.deadlineAt != null) parts.add("截止：" + formatFull(ev.deadlineAt!!))
        if (ev.location.isNotBlank()) parts.add("地点：" + ev.location)
        if (ev.note.isNotBlank()) parts.add(ev.note)
        if (ev.link.isNotBlank()) parts.add(ev.link)
        parts.add("由校程导出")
        return parts.joinToString("\n")
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")

    fun writeToFile(context: Context, events: List<ScheduleEvent>): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "xiaocheng-" + System.currentTimeMillis() + ".ics")
        file.writeText(build(events), Charsets.UTF_8)
        return file
    }
}
