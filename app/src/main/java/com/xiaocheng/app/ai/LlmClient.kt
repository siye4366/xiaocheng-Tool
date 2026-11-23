package com.xiaocheng.app.ai

import com.xiaocheng.app.data.ExtractedEvent
import com.xiaocheng.app.data.NoticeItem
import com.xiaocheng.app.net.PageLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class LlmConfig(val baseUrl: String, val apiKey: String, val model: String)

data class ChatTurn(val role: String, val content: String)

/** 调用 OpenAI 兼容的 Chat Completions 接口，把非结构化文本转成事件 JSON。 */
class LlmClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun extract(text: String, config: LlmConfig, now: LocalDateTime = LocalDateTime.now(), titleHint: String? = null): List<ExtractedEvent> =
        withContext(Dispatchers.IO) {
            if (config.apiKey.isBlank()) throw IOException("未配置 API Key")
            val endpoint = endpointOf(config.baseUrl)
            val body = JSONObject().apply {
                put("model", config.model)
                put("temperature", 0.1)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    put(JSONObject().put("role", "user").put("content", userPrompt(text, now, titleHint)))
                })
            }
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer " + config.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON))
                .build()
            val raw = http.newCall(req).execute().use { resp ->
                val content = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IOException("接口返回 " + resp.code + "：" + content.take(240))
                }
                content
            }
            parseResponse(raw)
        }

    /** 综合版对话：system + 最近历史 + 当前消息，返回模型原始文本（约定为 JSON）。 */
    suspend fun chat(
        config: LlmConfig,
        systemPrompt: String,
        history: List<ChatTurn>,
        userMessage: String,
    ): String = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) throw IOException("未配置 API Key")
        val messages = org.json.JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.takeLast(12).forEach {
            messages.put(JSONObject().put("role", it.role).put("content", it.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userMessage))
        val body = JSONObject().apply {
            put("model", config.model)
            put("temperature", 0.3)
            put("max_tokens", 700)
            put("messages", messages)
        }
        postChat(endpointOf(config.baseUrl), config, body)
    }

    /** 让模型整理一个网页上的通知/活动条目。 */
    suspend fun digestNotices(
        config: LlmConfig,
        pageUrl: String,
        pageTitle: String,
        pageText: String,
        links: List<PageLink>,
        query: String,
    ): List<NoticeItem> = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) throw IOException("未配置 API Key")
        val linkText = links.take(80).joinToString("\n") { "- " + it.title.take(100) + " | " + it.url }
        val user = "页面标题：" + pageTitle +
            "\n页面地址：" + pageUrl +
            "\n用户想找：" + (query.ifBlank { "最近的通知、活动、讲座、竞赛、截止日期等" }) +
            "\n\n页面正文（可能不完整）：\n" + pageText.take(6000) +
            "\n\n页面上的链接：\n" + linkText
        val system = """你是校园通知整理助手。请从给定网页正文和链接中，找出用户关心的通知/活动条目。
只输出严格 JSON，不要输出解释或 Markdown：
{"items":[{"title":"条目标题","url":"绝对链接或空字符串","date":"页面上的日期线索或空字符串","summary":"一句话说明"}]}
规则：
1. 只收录确实出现在页面中的条目，不要编造。
2. 优先保留有明确标题的条目；忽略导航、登录、页脚、广告。
3. url 必须来自给定链接列表，或能从页面地址推导；不确定就留空。
4. 最多 30 条。"""
        val body = JSONObject().apply {
            put("model", config.model)
            put("temperature", 0.1)
            put("max_tokens", 1600)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", user))
            })
        }
        val content = postChat(endpointOf(config.baseUrl), config, body)
        val json = extractJsonObject(content)
        val arr = json.optJSONArray("items") ?: org.json.JSONArray()
        val out = ArrayList<NoticeItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            if (title.isBlank()) continue
            val link = resolveUrl(pageUrl, o.optString("url").trim())
            out.add(NoticeItem(title, link, o.optString("date").trim()))
        }
        out
    }

    /** 查询接口额度，支持 DeepSeek / 部分 OpenAI 兼容服务；不支持时抛错。 */
    suspend fun fetchBalance(config: LlmConfig): String = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) throw IOException("未配置 API Key")
        val root = config.baseUrl.trim().trimEnd('/')
            .removeSuffix("/chat/completions")
            .removeSuffix("/v1")
        val urls = listOf(root + "/user/balance", root + "/dashboard/billing/credit_grants")
        var lastError = ""
        for (url in urls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.apiKey)
                    .get()
                    .build()
                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        lastError = "HTTP " + resp.code
                        return@use
                    }
                    val formatted = formatBalance(text)
                    if (formatted != null) return@withContext formatted
                    lastError = "接口返回格式无法识别"
                }
            } catch (e: Exception) {
                lastError = e.message ?: "网络错误"
            }
        }
        throw IOException(lastError.ifBlank { "该接口不支持额度查询" })
    }

    suspend fun testConnection(config: LlmConfig): String = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) throw IOException("未配置 API Key")
        val root = config.baseUrl.trim().trimEnd('/')
            .removeSuffix("/chat/completions")
            .removeSuffix("/v1")
        val req = Request.Builder()
            .url(root + "/models")
            .addHeader("Authorization", "Bearer " + config.apiKey)
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("接口返回 " + resp.code + "：" + resp.body?.string().orEmpty().take(160))
            }
        }
        "连接正常"
    }

    private fun postChat(endpoint: String, config: LlmConfig, body: JSONObject): String {
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer " + config.apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("接口返回 " + resp.code + "：" + raw.take(240))
            }
            val choices = JSONObject(raw).optJSONArray("choices")
                ?: throw IOException("接口返回缺少 choices")
            if (choices.length() == 0) throw IOException("接口返回为空")
            choices.getJSONObject(0).optJSONObject("message")?.optString("content").orEmpty()
                .ifBlank { throw IOException("模型没有返回内容") }
        }
    }

    private fun formatBalance(raw: String): String? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (root.has("balance_infos")) {
            val arr = root.optJSONArray("balance_infos") ?: return null
            val parts = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val currency = o.optString("currency", "")
                val total = o.optString("total_balance", "")
                val granted = o.optString("granted_balance", "")
                val topped = o.optString("topped_up_balance", "")
                if (total.isNotBlank()) {
                    var one = "余额 " + total + " " + currency
                    if (topped.isNotBlank()) one += "（充值 " + topped + "）"
                    if (granted.isNotBlank()) one += "（赠金 " + granted + "）"
                    parts.add(one.trim())
                }
            }
            if (parts.isNotEmpty()) return parts.joinToString("；")
        }
        val available = root.optDouble("total_available", Double.NaN)
        if (!available.isNaN()) {
            val used = root.optDouble("total_used", Double.NaN)
            return "可用额度 " + available + (if (used.isNaN()) "" else "（已用 " + used + "）")
        }
        return null
    }

    private fun resolveUrl(pageUrl: String, link: String): String {
        if (link.isBlank()) return ""
        return runCatching { java.net.URI(pageUrl).resolve(link).toString() }.getOrDefault(link)
    }

    private fun endpointOf(base: String): String {
        val b = base.trim().trimEnd('/')
        if (b.endsWith("/chat/completions")) return b
        return b + "/chat/completions"
    }

    private fun parseResponse(raw: String): List<ExtractedEvent> {
        val root = JSONObject(raw)
        val choices = root.optJSONArray("choices") ?: throw IOException("接口返回缺少 choices")
        if (choices.length() == 0) throw IOException("接口返回为空")
        val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content").orEmpty()
        if (content.isBlank()) throw IOException("模型没有返回内容")
        val json = extractJsonObject(content)
        val arr = json.optJSONArray("events") ?: json.optJSONArray("items") ?: org.json.JSONArray()
        val out = ArrayList<ExtractedEvent>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            if (title.isBlank()) continue
            out.add(
                ExtractedEvent(
                    title = title,
                    startAt = parseTime(o.optString("start_time")),
                    deadlineAt = parseTime(o.optString("deadline")),
                    location = o.optString("location").trim(),
                    link = o.optString("link").trim(),
                    category = normalizeCategory(o.optString("category")),
                    note = o.optString("description").trim(),
                )
            )
        }
        if (out.isEmpty()) throw IOException("模型没有抽取到事件")
        return out
    }

    private fun extractJsonObject(content: String): JSONObject {
        val cleaned = content.replace("\\u0060", "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) throw IOException("模型返回不是合法 JSON")
        return JSONObject(cleaned.substring(start, end + 1))
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private const val SYSTEM_PROMPT = """你是一个校园信息抽取助手。用户会给你一段来自班级群、社团群、公众号或学校官网的通知文本。
请抽取出其中所有可以行动的事件（活动、讲座、竞赛、报名、考试、会议等），输出严格 JSON，不要输出任何解释或 Markdown 代码块。

JSON 结构：
{"events":[{"title":"事件名称","start_time":"YYYY-MM-DD HH:mm 或 null","deadline":"YYYY-MM-DD HH:mm 或 null","location":"地点或空字符串","link":"链接或空字符串","category":"竞赛|讲座|活动|就业|志愿|考试|其他","description":"一句话补充说明"}]}

规则：
1. 结合用户给出的“当前时间”解析“今天、明天、下周三、本月底”等相对时间。
2. 没有年份时使用当前年份；已经过去的日期如果是年度常规活动，可理解为下一年。
3. 只有日期没有具体时间时：活动开始时间按 09:00，截止时间按 23:59。
4. “报名截止、提交截止、DDL、之前”对应 deadline；活动举办时间对应 start_time。两者可以同时存在。
5. 一条通知里如果有多个独立事件，请拆成多条；如果只是同一事件的时间、地点、报名信息，请合并成一条。
6. 不要编造文本中不存在的信息；无法确定的字段填 null 或空字符串。
7. category 只能从给定枚举中选择。"""

        private fun userPrompt(text: String, now: LocalDateTime, titleHint: String?): String {
            val zh = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val date = now.toLocalDate()
            val titleLine = if (titleHint.isNullOrBlank()) "" else "文章标题：" + titleHint + "\n"
            return "当前时间：" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                " " + zh[date.dayOfWeek.value - 1] + " " + now.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
                titleLine + "\n通知原文：\n" + text.take(8000)
        }

        fun parseTime(s: String?): Long? {
            if (s.isNullOrBlank() || s == "null") return null
            val v = s.trim().replace("年", "-").replace("月", "-").replace("日", " ")
                .replace("T", " ")
            val zone = ZoneId.systemDefault()
            runCatching { return OffsetDateTime.parse(s.trim()).toInstant().toEpochMilli() }
            runCatching { return java.time.Instant.parse(s.trim()).toEpochMilli() }
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "yyyy/MM/dd HH:mm", "yyyy/MM/dd"
            )
            for (p in patterns) {
                runCatching {
                    val fmt = DateTimeFormatter.ofPattern(p)
                    val dt = if (p.contains("H")) LocalDateTime.parse(v, fmt) else LocalDate.parse(v, fmt).atStartOfDay()
                    return dt.atZone(zone).toInstant().toEpochMilli()
                }
            }
            return null
        }

        fun normalizeCategory(s: String): String {
            val v = s.trim()
            val allowed = listOf("竞赛", "讲座", "活动", "就业", "志愿", "考试", "其他")
            return allowed.firstOrNull { v.contains(it) } ?: "其他"
        }
    }
}