package com.xiaocheng.app.data

data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val keyUrl: String,
    val note: String,
)

/** 内置的免费/低成本 OpenAI 兼容接口；Key 由用户自己申请，不内置共享 Key。 */
object AiProviders {
    val all = listOf(
        AiProvider(
            "zhipu", "智谱", "https://open.bigmodel.cn/api/paas/v4",
            "glm-4-flash", "https://open.bigmodel.cn/usercenter/apikeys",
            "国内 · GLM-4-Flash 免费",
        ),
        AiProvider(
            "deepseek", "DeepSeek", "https://api.deepseek.com/v1",
            "deepseek-chat", "https://platform.deepseek.com/api_keys",
            "国内 · 注册送额度",
        ),
        AiProvider(
            "groq", "Groq", "https://api.groq.com/openai/v1",
            "llama-3.3-70b-versatile", "https://console.groq.com/keys",
            "国外 · 免费额度",
        ),
        AiProvider(
            "gemini", "Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/",
            "gemini-2.0-flash", "https://aistudio.google.com/app/apikey",
            "国外 · 免费额度",
        ),
    )

    fun match(baseUrl: String, model: String): AiProvider? =
        all.firstOrNull { it.baseUrl == baseUrl.trim() && it.model == model.trim() }

    fun nameOf(baseUrl: String, model: String): String =
        match(baseUrl, model)?.name ?: "自定义"
}