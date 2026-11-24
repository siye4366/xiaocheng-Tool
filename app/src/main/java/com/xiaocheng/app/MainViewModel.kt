package com.xiaocheng.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocheng.app.ai.ChatTurn
import com.xiaocheng.app.ai.ExtractionService
import com.xiaocheng.app.ai.LlmClient
import com.xiaocheng.app.ai.LlmConfig
import com.xiaocheng.app.data.ExtractedEvent
import com.xiaocheng.app.data.NoticeItem
import com.xiaocheng.app.data.Prefs
import com.xiaocheng.app.data.ScheduleEvent
import com.xiaocheng.app.data.ScheduleRepository
import com.xiaocheng.app.export.IcsExporter
import com.xiaocheng.app.net.PageFetcher
import androidx.core.content.FileProvider
import com.xiaocheng.app.net.ApkUpdater
import com.xiaocheng.app.net.NetworkTime
import com.xiaocheng.app.net.UpdateChecker
import com.xiaocheng.app.net.WebSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AiUiState(
    val loading: Boolean = false,
    val engine: String = "",
    val results: List<ExtractedEvent> = emptyList(),
    val warning: String? = null,
    val error: String? = null,
    val sourceLabel: String = "",
)

data class UpdateUiState(
    val loading: Boolean = false,
    val url: String = "",
    val items: List<NoticeItem> = emptyList(),
    val firstRun: Boolean = false,
    val total: Int = 0,
    val error: String? = null,
    val engine: String = "",
    val readerUsed: Boolean = false,
    val pageTitle: String = "",
)

data class AiAction(
    val type: String,
    val id: String = "",
    val fields: Map<String, String?> = emptyMap(),
)

data class ChatMessage(
    val role: String,
    val content: String,
    val time: Long = System.currentTimeMillis(),
)

data class QuotaUiState(
    val loading: Boolean = false,
    val text: String = "",
    val error: String? = null,
)

data class UpdateCheckState(
    val show: Boolean = false,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val latest: String? = null,
    val url: String = "",
    val apkUrl: String = "",
    val notes: String = "",
    val error: String? = null,
    val isNew: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val extractor = ExtractionService(prefs)
    private val fetcher = PageFetcher()
    private val llm = LlmClient()
    private val updateChecker = UpdateChecker()
    private val webSearch = WebSearch()
    private val networkTime = NetworkTime()
    private val apkUpdater = ApkUpdater()

    val repo = ScheduleRepository(prefs)
    val events = repo.events

    var userName by mutableStateOf(prefs.userName.ifBlank { "同学" })
        private set
    var apiBase by mutableStateOf(prefs.apiBase)
        private set
    var apiKey by mutableStateOf(prefs.apiKey)
        private set
    var apiModel by mutableStateOf(prefs.apiModel)
        private set

    var aiState by mutableStateOf(AiUiState())
        private set
    var updateState by mutableStateOf(UpdateUiState())
        private set
    var toast by mutableStateOf<String?>(null)
        private set

    var useLocalEngine by mutableStateOf(prefs.useLocalEngine)
        private set
    var aiMode by mutableStateOf(prefs.aiMode)
        private set
    var aiRequirements by mutableStateOf(prefs.aiRequirements)
        private set
    var autoApplyActions by mutableStateOf(prefs.autoApplyActions)
        private set
    var webSearchEnabled by mutableStateOf(prefs.webSearchEnabled)
        private set

    var quotaState by mutableStateOf(QuotaUiState())
        private set
    var updateCheck by mutableStateOf(UpdateCheckState())
        private set

    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var chatLoading by mutableStateOf(false)
        private set
    var chatError by mutableStateOf<String?>(null)
        private set
    var pendingActions by mutableStateOf<List<AiAction>>(emptyList())
        private set
    var pendingDescriptions by mutableStateOf<List<String>>(emptyList())
        private set

    val hasApiKey: Boolean get() = apiKey.isNotBlank()
    val effectiveLocal: Boolean get() = !hasApiKey || useLocalEngine

    init {
        repo.seedExamplesIfNeeded()
    }

    fun consumeToast() { toast = null }

    fun saveUserName(name: String) {
        val v = name.trim().ifBlank { "同学" }
        userName = v
        prefs.userName = v
        toast = "已保存"
    }

    fun saveApi(base: String, key: String, model: String) {
        val hadKey = apiKey.isNotBlank()
        apiBase = base.trim().ifBlank { "https://api.deepseek.com/v1" }
        apiKey = key.trim()
        apiModel = model.trim().ifBlank { "deepseek-chat" }
        prefs.apiBase = apiBase
        prefs.apiKey = apiKey
        prefs.apiModel = apiModel
        // 第一次接入 API 时默认仍用本地规则；用户主动打开开关才走大模型。
        if (!hadKey || apiKey.isBlank()) {
            useLocalEngine = true
            prefs.useLocalEngine = true
        }
        if (apiKey.isBlank() && aiMode == "combined") {
            aiMode = "classic"
            prefs.aiMode = aiMode
        }
        toast = "已保存"
    }

    fun clearApi() {
        apiKey = ""
        prefs.apiKey = ""
        useLocalEngine = true
        prefs.useLocalEngine = true
        aiMode = "classic"
        prefs.aiMode = aiMode
        toast = "已清除 API Key，将使用本地规则"
    }

    fun switchEngine(local: Boolean) {
        if (local && !hasApiKey) {
            toast = "还没有配置 API，只能使用本地规则"
            return
        }
        useLocalEngine = local
        prefs.useLocalEngine = local
        if (local && aiMode == "combined") {
            aiMode = "classic"
            prefs.aiMode = aiMode
            toast = "综合版需要大模型，已切回经典模式"
        } else {
            toast = if (local) "已切换到本地规则" else "已切换到大模型"
        }
    }

    fun selectAiMode(mode: String) {
        if (mode == "combined" && !hasApiKey) {
            toast = "先配置 API"
            return
        }
        // 综合版固定用大模型；不会改动经典版的本地/大模型开关。
        aiMode = mode
        prefs.aiMode = mode
    }

    fun saveAiRequirements(text: String) {
        aiRequirements = text.trim()
        prefs.aiRequirements = aiRequirements
        toast = "已保存"
    }

    fun changeAutoApply(value: Boolean) {
        autoApplyActions = value
        prefs.autoApplyActions = value
    }

    fun setWebSearch(value: Boolean) {
        webSearchEnabled = value
        prefs.webSearchEnabled = value
        toast = "已保存"
    }

    fun toggleWebSearch() {
        setWebSearch(!webSearchEnabled)
    }

    fun clearAiResult() {
        aiState = AiUiState()
    }

    fun clearAiNotice() {
        if (aiState.error != null || aiState.warning != null) {
            aiState = aiState.copy(error = null, warning = null)
        }
        if (updateState.error != null) {
            updateState = updateState.copy(error = null)
        }
    }

    private fun llmConfig() = LlmConfig(apiBase, apiKey, apiModel)

    fun extractText(text: String) {
        if (text.isBlank()) {
            aiState = AiUiState(error = "先粘贴一段通知内容吧")
            return
        }
        viewModelScope.launch {
            aiState = AiUiState(loading = true)
            try {
                val result = extractor.extract(text, forceLocal = effectiveLocal)
                aiState = AiUiState(
                    engine = result.engine,
                    results = result.events,
                    warning = result.warning,
                    sourceLabel = "粘贴文本",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                aiState = AiUiState(error = e.message ?: "提取失败，请稍后重试")
            }
        }
    }

    fun extractUrl(url: String) {
        if (url.isBlank()) {
            aiState = AiUiState(error = "先填写一个文章链接")
            return
        }
        viewModelScope.launch {
            aiState = AiUiState(loading = true)
            try {
                val article = fetcher.fetchArticle(url)
                val result = extractor.extract(article.text, titleHint = article.title, forceLocal = effectiveLocal)
                aiState = AiUiState(
                    engine = result.engine,
                    results = result.events,
                    warning = result.warning,
                    sourceLabel = article.title,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                aiState = AiUiState(error = e.message ?: "链接解析失败")
            }
        }
    }

    fun extractNotice(item: NoticeItem) {
        viewModelScope.launch {
            aiState = AiUiState(loading = true, sourceLabel = item.title)
            try {
                val article = fetcher.fetchArticle(item.url)
                val result = extractor.extract(article.text, titleHint = article.title)
                aiState = AiUiState(
                    engine = result.engine,
                    results = result.events,
                    warning = result.warning,
                    sourceLabel = item.title,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                aiState = AiUiState(error = e.message ?: "条目解析失败")
            }
        }
    }

    fun checkUpdates(url: String, query: String = "", manualText: String = "") {
        if (url.isBlank() && manualText.isBlank()) {
            updateState = UpdateUiState(error = "先填写一个公开的通知页面链接，或粘贴登录后的正文")
            return
        }
        if (manualText.isNotBlank()) {
            if (!hasApiKey) {
                updateState = UpdateUiState(error = "整理登录后正文需要先配置 API")
                return
            }
            if (effectiveLocal) {
                updateState = UpdateUiState(error = "请先在 AI 页切换到大模型，再整理登录后正文")
                return
            }
            viewModelScope.launch {
                updateState = UpdateUiState(loading = true, url = url)
                try {
                    val aiItems = llm.digestNotices(
                        llmConfig(),
                        url.ifBlank { "manual" },
                        "登录后正文",
                        manualText,
                        emptyList(),
                        query,
                    )
                    if (aiItems.isEmpty()) throw IOException("没有从正文中整理出条目")
                    val key = url.ifBlank { "manual" }
                    val keys = aiItems.map { it.title + "|" + it.url }.toSet()
                    val old = prefs.loadSnapshot(key)
                    val fresh = if (old == null) aiItems.take(20) else aiItems.filter {
                        (it.title + "|" + it.url) !in old
                    }.take(20)
                    prefs.saveSnapshot(key, keys)
                    updateState = UpdateUiState(
                        url = url,
                        items = fresh,
                        firstRun = old == null,
                        total = aiItems.size,
                        engine = "AI 整理（粘贴正文）",
                        pageTitle = "登录后正文",
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    updateState = UpdateUiState(url = url, error = e.message ?: "整理正文失败")
                }
            }
            return
        }
        viewModelScope.launch {
            updateState = UpdateUiState(loading = true, url = url)
            try {
                var items: List<NoticeItem>
                var engine = "网页解析"
                var readerUsed = false
                var pageTitle = ""
                if (!effectiveLocal) {
                    try {
                        val page = fetcher.fetchPageContent(url)
                        pageTitle = page.title
                        readerUsed = page.usedReader
                        val aiItems = try {
                            llm.digestNotices(llmConfig(), url, page.title, page.text, page.links, query)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            emptyList()
                        }
                        if (aiItems.isNotEmpty()) {
                            items = aiItems
                            engine = "AI 整理"
                        } else {
                            items = fetcher.fetchNoticeItems(url)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        items = try {
                            fetcher.fetchNoticeItems(url)
                        } catch (inner: Exception) {
                            throw IOException("页面没有拿到可读内容，可能是动态渲染或需要登录。可以换通知列表页或文章详情页再试。")
                        }
                    }
                } else {
                    items = try {
                        fetcher.fetchNoticeItems(url)
                    } catch (inner: Exception) {
                        throw IOException("页面没有拿到可读内容，可能是动态渲染或需要登录。可以换通知列表页或文章详情页再试。")
                    }
                }
                val keys = items.map { it.title + "|" + it.url }.toSet()
                val old = prefs.loadSnapshot(url)
                val fresh = if (old == null) items.take(20) else items.filter {
                    (it.title + "|" + it.url) !in old
                }.take(20)
                prefs.saveSnapshot(url, keys)
                updateState = UpdateUiState(
                    url = url,
                    items = fresh,
                    firstRun = old == null,
                    total = items.size,
                    engine = engine,
                    readerUsed = readerUsed,
                    pageTitle = pageTitle,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState = UpdateUiState(url = url, error = e.message ?: "检查更新失败")
            }
        }
    }

    fun refreshQuota() {
        if (!hasApiKey) {
            quotaState = QuotaUiState(error = "先配置 API Key")
            return
        }
        viewModelScope.launch {
            quotaState = QuotaUiState(loading = true)
            try {
                val text = llm.fetchBalance(llmConfig())
                quotaState = QuotaUiState(text = text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                quotaState = QuotaUiState(error = e.message ?: "额度查询失败")
            }
        }
    }

    fun testApi() {
        if (!hasApiKey) {
            toast = "先配置 API Key"
            return
        }
        viewModelScope.launch {
            try {
                toast = llm.testConnection(llmConfig())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast = "连接失败：" + (e.message ?: "未知错误")
            }
        }
    }

    fun checkForUpdates() {
        updateCheck = UpdateCheckState(show = true, checking = true)
        viewModelScope.launch {
            try {
                val current = runCatching {
                    getApplication<Application>().packageManager
                        .getPackageInfo(getApplication<Application>().packageName, 0).versionName
                }.getOrNull() ?: "1.2.2"
                val info = updateChecker.check(current)
                updateCheck = if (info == null) {
                    UpdateCheckState(show = true, latest = current)
                } else {
                    UpdateCheckState(
                        show = true,
                        latest = info.version,
                        url = info.url,
                        apkUrl = info.apkUrl,
                        notes = info.notes,
                        isNew = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateCheck = UpdateCheckState(show = true, error = e.message ?: "检查更新失败")
            }
        }
    }

    fun dismissUpdateCheck() {
        updateCheck = UpdateCheckState()
    }

    fun downloadUpdate(context: Context) {
        val state = updateCheck
        if (state.downloading || state.apkUrl.isBlank()) return
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            toast = "请先允许安装未知应用"
            return
        }
        updateCheck = state.copy(downloading = true, error = null)
        viewModelScope.launch {
            try {
                val file = apkUpdater.download(context, state.apkUrl)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                val install = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(install)
                updateCheck = UpdateCheckState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateCheck = updateCheck.copy(
                    downloading = false,
                    error = "下载失败：" + (e.message ?: "未知错误"),
                )
            }
        }
    }

    fun clearChat() {
        chatMessages = emptyList()
        pendingActions = emptyList()
        pendingDescriptions = emptyList()
        chatError = null
    }

    fun discardPendingActions() {
        if (pendingActions.isEmpty()) return
        pendingActions = emptyList()
        pendingDescriptions = emptyList()
        chatMessages = chatMessages + ChatMessage("assistant", "好的，这次修改已取消。")
    }

    fun applyPendingActions() {
        val actions = pendingActions
        if (actions.isEmpty()) return
        val results = executeActions(actions)
        pendingActions = emptyList()
        pendingDescriptions = emptyList()
        chatMessages = chatMessages + ChatMessage(
            "assistant",
            "已应用修改：\n" + results.joinToString("\n"),
        )
        toast = "已应用 " + actions.size + " 个日程操作"
    }

    fun sendChat(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        if (!hasApiKey) {
            chatError = "请先在「个人 → AI 配置」里填写 API Key"
            return
        }
        // 综合版始终使用 API；经典版的本地/大模型开关与这里无关。
        if (!hasApiKey) {
            chatError = "先配置 API"
            return
        }
        chatError = null
        pendingActions = emptyList()
        pendingDescriptions = emptyList()
        chatMessages = chatMessages + ChatMessage("user", message)
        chatLoading = true
        viewModelScope.launch {
            try {
                val now = networkTime.now() ?: LocalDateTime.now()
                if (isDateTimeQuestion(message)) {
                    chatMessages = chatMessages + ChatMessage(
                        "assistant",
                        "今天是 " + now.format(DateTimeFormatter.ofPattern("yyyy年M月d日，EEEE", Locale.CHINA)) +
                            "，当前时间 " + now.format(DateTimeFormatter.ofPattern("HH:mm")) + "。",
                    )
                    return@launch
                }
                val pageUrl = Regex("""https?://[^\s，。；）)]+""").find(message)?.value
                val page = pageUrl?.let {
                    runCatching { fetcher.fetchPageContent(it) }.getOrNull()
                }
                val userContent = if (page != null) {
                    message + "\n\n[网页内容：" + page.title + "]\n" +
                        page.text.take(5000) +
                        if (page.links.isNotEmpty()) {
                            "\n\n页面链接：\n" + page.links.take(40).joinToString("\n") { "- " + it.title + " | " + it.url }
                        } else ""
                } else message
                val history = chatMessages.dropLast(1)
                    .filter { it.role == "user" || it.role == "assistant" }
                    .map { ChatTurn(it.role, it.content) }
                val raw = llm.chat(llmConfig(), buildChatSystemPrompt(now), history, userContent)
                val json = runCatching { parseJsonObject(raw) }.getOrNull()
                var reply = json?.optString("reply")?.trim().orEmpty().ifBlank { raw.trim() }
                val actions = json?.let { parseActions(it) } ?: emptyList()
                if (actions.isNotEmpty()) {
                    val descriptions = actions.map { describeAction(it) }
                    if (autoApplyActions) {
                        val results = executeActions(actions)
                        reply += "\n\n" + results.joinToString("\n")
                    } else {
                        pendingActions = actions
                        pendingDescriptions = descriptions
                        reply += "\n\n准备执行 " + actions.size + " 个操作：\n" +
                            descriptions.joinToString("\n") +
                            "\n\n确认后点下方「应用修改」。"
                    }
                }
                chatMessages = chatMessages + ChatMessage("assistant", reply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                chatError = e.message ?: "对话失败"
            } finally {
                chatLoading = false
            }
        }
    }

    private fun parseJsonObject(raw: String): JSONObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    private fun parseActions(json: JSONObject): List<AiAction> {
        val arr = json.optJSONArray("actions") ?: return emptyList()
        val out = ArrayList<AiAction>()
        val keys = listOf("title", "start", "deadline", "location", "link", "category", "note")
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val type = o.optString("type").trim()
            if (type !in listOf("add", "update", "delete", "complete", "uncomplete")) continue
            val fieldsObj = o.optJSONObject("fields") ?: o
            val fields = LinkedHashMap<String, String?>()
            for (key in keys) {
                if (fieldsObj.has(key)) {
                    fields[key] = if (fieldsObj.isNull(key)) null else fieldsObj.optString(key)
                }
            }
            if (!fields.containsKey("start") && fieldsObj.has("start_time")) {
                fields["start"] = if (fieldsObj.isNull("start_time")) null else fieldsObj.optString("start_time")
            }
            if (!fields.containsKey("deadline") && fieldsObj.has("end_time")) {
                fields["deadline"] = if (fieldsObj.isNull("end_time")) null else fieldsObj.optString("end_time")
            }
            out.add(AiAction(type, o.optString("id").trim(), fields))
        }
        return out
    }

    private fun describeAction(action: AiAction): String {
        val title = events.value.firstOrNull { it.id == action.id }?.title
        return when (action.type) {
            "add" -> "新增日程：" + (action.fields["title"] ?: "未命名")
            "update" -> "修改日程：" + (title ?: action.id)
            "delete" -> "删除日程：" + (title ?: action.id)
            "complete" -> "标记完成：" + (title ?: action.id)
            "uncomplete" -> "取消完成：" + (title ?: action.id)
            else -> action.type
        }
    }

    private fun executeActions(actions: List<AiAction>): List<String> {
        val results = ArrayList<String>()
        for (action in actions) {
            val fields = action.fields
            when (action.type) {
                "add" -> {
                    val title = fields["title"]?.trim().orEmpty()
                    if (title.isBlank()) {
                        results.add("跳过：新增日程缺少标题")
                        continue
                    }
                    repo.add(
                        ScheduleEvent(
                            title = title,
                            startAt = LlmClient.parseTime(fields["start"]),
                            deadlineAt = LlmClient.parseTime(fields["deadline"]),
                            location = fields["location"].orEmpty(),
                            link = fields["link"].orEmpty(),
                            category = LlmClient.normalizeCategory(fields["category"].orEmpty()),
                            note = fields["note"].orEmpty(),
                            source = "AI 对话",
                        )
                    )
                    results.add("已新增：" + title)
                }
                "update" -> {
                    val ev = events.value.firstOrNull { it.id == action.id }
                    if (ev == null) {
                        results.add("未找到日程：" + action.id)
                        continue
                    }
                    if (fields.containsKey("title")) ev.title = fields["title"] ?: ev.title
                    if (fields.containsKey("start")) ev.startAt = LlmClient.parseTime(fields["start"])
                    if (fields.containsKey("deadline")) ev.deadlineAt = LlmClient.parseTime(fields["deadline"])
                    if (fields.containsKey("location")) ev.location = fields["location"].orEmpty()
                    if (fields.containsKey("link")) ev.link = fields["link"].orEmpty()
                    if (fields.containsKey("category")) {
                        ev.category = LlmClient.normalizeCategory(fields["category"].orEmpty())
                    }
                    if (fields.containsKey("note")) ev.note = fields["note"].orEmpty()
                    repo.update(ev)
                    results.add("已修改：" + ev.title)
                }
                "delete" -> {
                    val ev = events.value.firstOrNull { it.id == action.id }
                    if (ev == null) {
                        results.add("未找到日程：" + action.id)
                    } else {
                        repo.delete(ev.id)
                        results.add("已删除：" + ev.title)
                    }
                }
                "complete", "uncomplete" -> {
                    val ev = events.value.firstOrNull { it.id == action.id }
                    if (ev == null) {
                        results.add("未找到日程：" + action.id)
                    } else if (action.type == "complete") {
                        repo.markDone(setOf(ev.id))
                        results.add("已完成：" + ev.title)
                    } else {
                        repo.markUndone(setOf(ev.id))
                        results.add("已取消完成：" + ev.title)
                    }
                }
            }
        }
        return results
    }

    private fun buildChatSystemPrompt(now: LocalDateTime): String {
        val custom = if (aiRequirements.isBlank()) "" else "\n\n用户附加的常驻要求：\n" + aiRequirements.take(2000)
        val eventsText = events.value.take(80).joinToString("\n") {
            "- id=" + it.id + " | " + it.title + " | 开始=" + (it.startAt ?: "无") +
                " | 截止=" + (it.deadlineAt ?: "无") + " | 地点=" + it.location +
                " | 分类=" + it.category + " | 已完成=" + it.done
        }.ifBlank { "（暂无日程）" }
        return DEFAULT_PERSONA + custom +
            "\n\n现有日程：\n" + eventsText +
            "\n\n" + CHAT_ACTION_SCHEMA
    }

    private fun isDateTimeQuestion(text: String): Boolean {
        val t = text.replace(" ", "")
        return listOf(
            "今天几号", "今天日期", "今天星期", "现在几点", "现在时间", "几月几号",
            "当前时间", "whatdate", "whattime", "todaydate", "today'sdate",
        ).any { t.contains(it, ignoreCase = true) }
    }

    private fun shouldSearch(text: String): Boolean {
        val t = text.trim()
        if (t.length < 4) return false
        if (t.contains("http://") || t.contains("https://")) return false
        val scheduleWords = listOf("日程", "安排", "提醒", "新增", "添加", "删除", "完成", "取消", "改到", "改一下", "日程表")
        if (scheduleWords.any { t.contains(it) }) return false
        val triggers = listOf(
            "查", "搜", "最新", "新闻", "现在", "今天", "明天", "多少", "哪",
            "什么时候", "推荐", "天气", "价格", "是谁", "为什么", "怎么", "介绍", "翻译",
        )
        return triggers.any { t.contains(it) } || t.contains("?") || t.contains("？")
    }

    private suspend fun buildSearchContext(query: String): String {
        val results = runCatching { webSearch.search(query, 5) }.getOrDefault(emptyList())
        if (results.isEmpty()) return ""
        return "\n\n[联网搜索结果，可能不完整，请自行判断]\n" + results.joinToString("\n") {
            "- " + it.title + "\n  " + it.snippet.take(180) + "\n  " + it.url
        }
    }

    fun saveAllExtracted() {
        val list = aiState.results
        if (list.isEmpty()) return
        repo.addAll(list.map { it.toScheduleEvent("AI") })
        aiState = AiUiState()
        toast = "已保存 " + list.size + " 条日程"
    }

    fun saveOne(event: ExtractedEvent) {
        repo.add(event.toScheduleEvent("AI"))
        val left = aiState.results.filter { it !== event }
        aiState = aiState.copy(results = left)
        toast = "已保存到日程"
    }

    fun addManual(event: ScheduleEvent) {
        repo.add(event)
        toast = "日程已添加"
    }

    fun updateEvent(event: ScheduleEvent) {
        repo.update(event)
        toast = "日程已更新"
    }

    fun deleteEvent(id: String) {
        repo.delete(id)
        toast = "日程已删除"
    }

    fun deleteEvents(ids: Set<String>) {
        if (ids.isEmpty()) return
        repo.deleteMany(ids)
        toast = "已删除 " + ids.size + " 条日程"
    }

    fun markDone(ids: Set<String>) {
        if (ids.isEmpty()) return
        repo.markDone(ids)
        toast = "已标记 " + ids.size + " 条完成"
    }

    fun markUndone(ids: Set<String>) {
        if (ids.isEmpty()) return
        repo.markUndone(ids)
        toast = "已取消 " + ids.size + " 条的完成标记"
    }

    fun toggleDone(id: String) {
        repo.toggleDone(id)
    }

    fun clearEvents() {
        repo.clear()
        toast = "日程已清空"
    }

    fun buildIcsText(): String = IcsExporter.build(events.value)

    fun exportIcs(context: Context): File = IcsExporter.writeToFile(context, events.value)
}


private const val DEFAULT_PERSONA = """你是「校程」的日程助手，说话简洁、可靠，像一位细心的同学。
你可以正常聊天、解释、给建议，也可以整理校园通知、网页内容和日程的增删改查。
只有当问题明显与日程无关且需要大量计算或长篇专业处理（例如完整解数学题、写长代码、代写论文）时，才用一两句话礼貌拒绝，避免消耗 token；普通问好、闲聊、简单问答可以正常回应。
不要编造日程或网页内容。用户要求修改日程时，只能使用给定日程列表里的 id；找不到就明确说没找到。
回答尽量短，优先通过 actions 完成操作，不要长篇解释。"""

private const val CHAT_ACTION_SCHEMA = """请严格输出 JSON，不要输出 Markdown 代码块：
{"reply":"给用户的简短中文回复","actions":[{"type":"add","fields":{"title":"事件名","start":"YYYY-MM-DD HH:mm 或 null","deadline":"YYYY-MM-DD HH:mm 或 null","location":"","link":"","category":"竞赛|讲座|活动|就业|志愿|考试|其他","note":""}},{"type":"update","id":"现有日程id","fields":{"title":"只填需要改的字段"}},{"type":"delete","id":"现有日程id"},{"type":"complete","id":"现有日程id"},{"type":"uncomplete","id":"现有日程id"}]}

规则：
1. 普通聊天、问候或不需要改日程时，actions 输出空数组。
2. 新增日程用 add；修改用 update，fields 只放要改的字段；删除用 delete；完成/取消完成用 complete/uncomplete。
3. 时间格式 YYYY-MM-DD HH:mm；没有时间填 null。不要编造 id。
4. reply 要简短自然，不要复述 JSON，不要输出 JSON 以外的内容。"""