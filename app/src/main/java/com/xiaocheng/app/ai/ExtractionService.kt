package com.xiaocheng.app.ai

import com.xiaocheng.app.data.ExtractionResult
import com.xiaocheng.app.data.Prefs
import kotlinx.coroutines.CancellationException

/** 统一入口：优先调用配置好的大模型，失败或未配置时回退到本地规则。 */
class ExtractionService(private val prefs: Prefs) {

    private val llm = LlmClient()

    suspend fun extract(text: String, titleHint: String? = null): ExtractionResult {
        val local = LocalExtractor.extract(text, titleHint = titleHint)
        val key = prefs.apiKey.trim()
        if (key.isBlank()) {
            return ExtractionResult(local, "本地规则")
        }
        return try {
            val events = llm.extract(
                text,
                LlmConfig(prefs.apiBase, key, prefs.apiModel),
                titleHint = titleHint,
            )
            if (events.isEmpty()) {
                ExtractionResult(local, "本地规则", warning = "大模型没有抽取到事件，已回退本地规则")
            } else {
                ExtractionResult(events, "大模型")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ExtractionResult(
                local,
                "本地规则",
                warning = "大模型调用失败，已回退本地规则：" + (e.message ?: "未知错误").take(160),
            )
        }
    }
}