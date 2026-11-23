package com.xiaocheng.app.ai

import com.xiaocheng.app.data.ExtractedEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

/**
 * 离线规则抽取器。没有配置大模型 API 时作为默认引擎，也作为大模型失败时的兜底。
 * 目前支持：今天/明天/后天、本周三/下周三、X月X日、X.X、X/X、具体时刻、地点、链接、分类、截止日期。
 */
object LocalExtractor {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val urlRegex = Regex("""https?://[^\s，。；、）)】]+""")
    private val timeRegex = Regex("""(?<ap>上午|下午|晚上|中午|早上|凌晨)?\s*(?<h>[0-9]{1,2})\s*[:：点时]\s*(?<mi>[0-9]{1,2})?\s*分?""")
    private val relativeRegex = Regex("""(大后天|今天|今日|明天|明日|后天|昨天|前天)""")
    private val yearDateRegex = Regex("""(?<y>[0-9]{4})\s*[年./-]\s*(?<m>[0-9]{1,2})\s*[月./-]\s*(?<d>[0-9]{1,2})\s*[日号]?""")
    private val monthDateRegex = Regex("""(?<m>[0-9]{1,2})\s*月\s*(?<d>[0-9]{1,2})\s*[日号]""")
    private val dotDateRegex = Regex("""(?<![0-9])(?<m>[0-9]{1,2})\s*[./]\s*(?<d>[0-9]{1,2})(?![0-9])""")
    private val weekRegex = Regex("""(?<p>本|这|下下|下)?\s*(?:周|星期|礼拜)\s*(?<w>[一二三四五六日天1-7])""")

    private data class DateHit(val start: Int, val end: Int, val date: LocalDate, val raw: String)

    fun extract(text: String, now: LocalDateTime = LocalDateTime.now(), titleHint: String? = null): List<ExtractedEvent> {
        val clean = text.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (clean.isBlank()) return emptyList()
        val blocks = splitBlocks(clean, now)
        val hint = if (blocks.size == 1) cleanTitleHint(titleHint) else null
        return blocks.mapNotNull { blockToEvent(it, now, hint) }
    }

    private fun splitBlocks(text: String, now: LocalDateTime): List<String> {
        val paragraphs = text.split(Regex("""\n\s*\n""")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.size >= 2 && paragraphs.count { findDateHits(it, now).isNotEmpty() } >= 2) {
            return paragraphs
        }
        val numbered = text.lines().map { it.trim() }
            .filter { it.isNotBlank() && Regex("""^(?:[0-9]{1,2}[、.．)]|[•·●○])\s*""").containsMatchIn(it) }
            .filter { findDateHits(it, now).isNotEmpty() }
        if (numbered.size >= 2) return numbered
        return listOf(text)
    }

    private fun blockToEvent(block: String, now: LocalDateTime, titleHint: String?): ExtractedEvent? {
        val title = titleHint ?: extractTitle(block)
        if (title.isBlank()) return null
        val link = urlRegex.find(block)?.value?.trimEnd('.', ',', '，', '。', '；', ';', ')', '）') ?: ""
        val location = extractLocation(block)
        val category = classify(block)
        val dates = findDateHits(block, now)

        if (dates.isEmpty()) {
            return ExtractedEvent(title, null, null, location, link, category, noteOf(block))
        }

        var start: Long? = null
        var deadline: Long? = null
        for (hit in dates) {
            val deadlineHit = isDeadlineContext(block, hit)
            val time = findTimeNear(block, hit)
            if (deadlineHit) {
                val t = time ?: LocalTime.of(23, 59)
                val ms = hit.date.atTime(t).atZone(zone).toInstant().toEpochMilli()
                if (deadline == null || ms < deadline!!) deadline = ms
            } else if (start == null) {
                val t = time ?: LocalTime.of(9, 0)
                start = hit.date.atTime(t).atZone(zone).toInstant().toEpochMilli()
            }
        }
        if (start == null && deadline == null) {
            val hit = dates.first()
            start = hit.date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        }
        return ExtractedEvent(title, start, deadline, location, link, category, noteOf(block))
    }

    private fun findDateHits(text: String, now: LocalDateTime): List<DateHit> {
        val hits = mutableListOf<DateHit>()

        relativeRegex.findAll(text).forEach { m ->
            val offset = when (m.value) {
                "今天", "今日" -> 0L
                "明天", "明日" -> 1L
                "后天" -> 2L
                "大后天" -> 3L
                "昨天" -> -1L
                "前天" -> -2L
                else -> 0L
            }
            hits.add(DateHit(m.range.first, m.range.last + 1, now.toLocalDate().plusDays(offset), m.value))
        }

        yearDateRegex.findAll(text).forEach { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
            val d = m.groupValues[3].toIntOrNull() ?: return@forEach
            val date = runCatching { LocalDate.of(y, mo, d) }.getOrNull() ?: return@forEach
            hits.add(DateHit(m.range.first, m.range.last + 1, date, m.value))
        }

        monthDateRegex.findAll(text).forEach { m ->
            val mo = m.groupValues[1].toIntOrNull() ?: return@forEach
            val d = m.groupValues[2].toIntOrNull() ?: return@forEach
            val date = runCatching { LocalDate.of(now.year, mo, d) }.getOrNull() ?: return@forEach
            hits.add(DateHit(m.range.first, m.range.last + 1, adjustYear(date, now), m.value))
        }

        dotDateRegex.findAll(text).forEach { m ->
            val mo = m.groupValues[1].toIntOrNull() ?: return@forEach
            val d = m.groupValues[2].toIntOrNull() ?: return@forEach
            if (mo !in 1..12 || d !in 1..31) return@forEach
            val date = runCatching { LocalDate.of(now.year, mo, d) }.getOrNull() ?: return@forEach
            hits.add(DateHit(m.range.first, m.range.last + 1, adjustYear(date, now), m.value))
        }

        weekRegex.findAll(text).forEach { m ->
            val prefix = m.groupValues[1]
            val w = m.groupValues[2]
            val day = when (w) {
                "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5; "六" -> 6
                "日", "天" -> 7; else -> w.toIntOrNull() ?: return@forEach
            }
            val monday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            var date = monday.plusDays((day - 1).toLong())
            when (prefix) {
                "下" -> date = date.plusWeeks(1)
                "下下" -> date = date.plusWeeks(2)
                "本", "这" -> Unit
                else -> if (date.isBefore(now.toLocalDate())) date = date.plusWeeks(1)
            }
            hits.add(DateHit(m.range.first, m.range.last + 1, date, m.value))
        }

        return dedupe(hits)
    }

    private fun adjustYear(date: LocalDate, now: LocalDateTime): LocalDate {
        var d = date
        if (d.isBefore(now.toLocalDate().minusDays(200))) d = d.plusYears(1)
        return d
    }

    private fun dedupe(hits: List<DateHit>): List<DateHit> {
        val sorted = hits.sortedWith(compareBy({ it.start }, { -(it.end - it.start) }))
        val out = mutableListOf<DateHit>()
        for (h in sorted) {
            if (out.none { h.start < it.end && h.end > it.start }) out.add(h)
        }
        val relativeWords = setOf("今天", "今日", "明天", "明日", "后天", "大后天", "昨天", "前天")
        val preferred = out.filter { h ->
            h.raw !in relativeWords || out.none { other ->
                other.raw !in relativeWords && abs(other.start - h.start) <= 12
            }
        }
        return preferred.sortedBy { it.start }
    }

    private fun isDeadlineContext(text: String, hit: DateHit): Boolean {
        val before = text.substring(maxOf(0, hit.start - 20), hit.start)
        val after = text.substring(hit.end, minOf(text.length, hit.end + 10))
        val strongWords = listOf("截止", "截至", "DDL", "ddl", "deadline", "Deadline", "之前")
        if (strongWords.any { before.contains(it) }) return true
        if (after.contains("之前")) return true
        if (after.contains("前") && !after.contains("前往")) return true
        return false
    }

    private fun findTimeNear(text: String, hit: DateHit): LocalTime? {
        val from = maxOf(0, hit.start - 14)
        val to = minOf(text.length, hit.end + 40)
        val region = text.substring(from, to)
        val matches = timeRegex.findAll(region).toList()
        if (matches.isEmpty()) return null
        val best = matches.minByOrNull { abs((from + it.range.first) - hit.end) } ?: return null
        val ap = best.groups["ap"]?.value.orEmpty()
        var hour = best.groups["h"]?.value?.toIntOrNull() ?: return null
        var minute = best.groups["mi"]?.value?.toIntOrNull() ?: 0
        val tail = region.substring(minOf(region.length, best.range.last + 1)).take(2)
        if (minute == 0 && tail.startsWith("半")) minute = 30
        if (ap == "下午" || ap == "晚上") {
            if (hour in 1..11) hour += 12
        } else if (ap == "中午") {
            if (hour in 1..10) hour += 12
        } else if (ap == "凌晨") {
            if (hour == 12) hour = 0
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    private fun extractTitle(block: String): String {
        val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
        val cleaned = lines.map { stripBullet(it) }
            .map { it.replace(Regex("""^[【\[][^】\]]{1,14}[】\]]\s*"""), "").trim() }
            .filter { it.isNotBlank() }
            .filterNot { Regex("""^(通知|公告|活动通知|讲座通知|比赛通知)$""").matches(it) }
        val banned = listOf("时间：", "时间:", "地点：", "地点:", "截止", "报名方式", "主办", "承办",
            "活动对象", "联系方式", "扫码", "注意", "活动时间", "活动地点", "报名时间")
        val candidates = cleaned.filter { line ->
            line.length in 3..60 &&
                !urlRegex.containsMatchIn(line) &&
                banned.none { line.startsWith(it) } &&
                !line.matches(Regex("""^[0-9\s年月日号./:：\-—~至到]+$"""))
        }
        var raw = candidates.firstOrNull() ?: cleaned.firstOrNull() ?: block.take(30)
        raw = stripBullet(raw).trim()
        raw = raw.replace(Regex("""\s*(?:时间|地点|报名截止|截止)\s*[:：].*$"""), "")
        val marker = Regex("""\s+(?=\d{1,2}月\d{1,2}日|\d{4}年|下周|本周|周[一二三四五六日天])""").find(raw)
        if (marker != null && marker.range.first >= 4) raw = raw.substring(0, marker.range.first)
        raw = raw.trim().trimEnd('。', '，', '；', ';', ':', '：')
        if (raw.length < 2) raw = block.take(24).trim()
        return if (raw.length > 40) raw.take(40) + "…" else raw
    }

    private fun cleanTitleHint(raw: String?): String? {
        var value = raw?.replace(Regex("""\s+"""), " ")?.trim().orEmpty()
        if (value.length !in 3..80) return null
        value = value.split(Regex("""\s+[-_|｜]\s+""")).first().trim()
        val idx = value.lastIndexOf('-')
        if (idx > 8) {
            val tail = value.substring(idx + 1).trim()
            val siteLike = tail.endsWith("大学") || tail.endsWith("学院") ||
                tail.endsWith("网") || tail.endsWith("官网") || tail.endsWith("新闻网")
            if (tail.length in 2..14 && tail.none { it.isDigit() } && siteLike) {
                value = value.substring(0, idx).trim()
            }
        }
        return value.take(60)
    }

    private fun stripBullet(s: String): String {
        var v = s.trim()
        v = v.replace(Regex("""^[•·●○\-—*]+\s*"""), "")
        v = v.replace(Regex("""^[0-9]{1,2}[、.．)）]\s*"""), "")
        return v.trim()
    }

    private fun extractLocation(text: String): String {
        val label = Regex("""(?:活动)?(?:地点|地址|位置|场地)\s*[：:]\s*([^\n，。；;]{2,40})""").find(text)
        if (label != null) return label.groupValues[1].trim().trimEnd('。', '，', '；')
        val lineLoc = text.lines().map { it.trim() }.firstOrNull {
            it.startsWith("地点") || it.startsWith("活动地点")
        }
        if (lineLoc != null) {
            return lineLoc.substringAfter("地点").trimStart('：', ':', ' ').take(40)
        }
        val inPlace = Regex("""(?:在|于)\s*([^\s，。；:：]{2,20}(?:报告厅|会议室|教室|楼|馆|中心|操场|体育场|礼堂))""").find(text)
        if (inPlace != null) return inPlace.groupValues[1]
        val known = Regex("""(图书馆|教学楼|实验楼|体育馆|操场|大学生活动中心|学术报告厅|腾讯会议|线上|钉钉)""").find(text)
        return known?.value ?: ""
    }

    private fun classify(text: String): String {
        val groups = listOf(
            "竞赛" to listOf("竞赛", "比赛", "大赛", "挑战赛", "创新创业", "数学建模", "程序设计"),
            "讲座" to listOf("讲座", "报告会", "论坛", "分享会", "学术报告", "宣讲"),
            "就业" to listOf("招聘", "宣讲会", "实习", "就业", "双选会", "岗位"),
            "志愿" to listOf("志愿", "公益", "义工", "支教"),
            "考试" to listOf("考试", "补考", "测验", "体测", "四六级", "期末"),
            "活动" to listOf("晚会", "演出", "社团", "运动会", "活动", "招新", "比赛日"),
        )
        var best = "其他"
        var bestScore = 0
        for ((name, words) in groups) {
            var score = 0
            for (w in words) score += countOf(text, w)
            if (score > bestScore) {
                bestScore = score
                best = name
            }
        }
        return best
    }

    private fun countOf(text: String, word: String): Int {
        var count = 0
        var index = text.indexOf(word)
        while (index >= 0) {
            count++
            index = text.indexOf(word, index + word.length)
        }
        return count
    }

    private fun noteOf(block: String): String {
        val oneLine = block.replace(Regex("""\s+"""), " ").trim()
        return if (oneLine.length > 600) oneLine.take(600) + "…" else oneLine
    }
}