package com.xiaocheng.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocheng.app.ai.ExtractionService
import com.xiaocheng.app.data.ExtractedEvent
import com.xiaocheng.app.data.NoticeItem
import com.xiaocheng.app.data.Prefs
import com.xiaocheng.app.data.ScheduleEvent
import com.xiaocheng.app.data.ScheduleRepository
import com.xiaocheng.app.export.IcsExporter
import com.xiaocheng.app.net.PageFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

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
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val extractor = ExtractionService(prefs)
    private val fetcher = PageFetcher()

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

    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    init {
        repo.seedExamplesIfNeeded()
    }

    fun consumeToast() { toast = null }

    fun saveUserName(name: String) {
        val v = name.trim().ifBlank { "同学" }
        userName = v
        prefs.userName = v
        toast = "名字已更新"
    }

    fun saveApi(base: String, key: String, model: String) {
        apiBase = base.trim().ifBlank { "https://api.deepseek.com/v1" }
        apiKey = key.trim()
        apiModel = model.trim().ifBlank { "deepseek-chat" }
        prefs.apiBase = apiBase
        prefs.apiKey = apiKey
        prefs.apiModel = apiModel
        toast = if (apiKey.isBlank()) "已切换为本地规则引擎" else "大模型配置已保存"
    }

    fun clearApi() {
        apiKey = ""
        prefs.apiKey = ""
        toast = "已清除 API Key，将使用本地规则"
    }

    fun extractText(text: String) {
        if (text.isBlank()) {
            aiState = AiUiState(error = "先粘贴一段通知内容吧")
            return
        }
        viewModelScope.launch {
            aiState = AiUiState(loading = true)
            try {
                val result = extractor.extract(text)
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
                val result = extractor.extract(article.text, titleHint = article.title)
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

    fun checkUpdates(url: String) {
        if (url.isBlank()) {
            updateState = UpdateUiState(error = "先填写一个公开的通知页面链接")
            return
        }
        viewModelScope.launch {
            updateState = UpdateUiState(loading = true, url = url)
            try {
                val items = fetcher.fetchNoticeItems(url)
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
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState = UpdateUiState(url = url, error = e.message ?: "检查更新失败")
            }
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