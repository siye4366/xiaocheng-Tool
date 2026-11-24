package com.xiaocheng.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 所有本地配置与轻量数据都存在 SharedPreferences 里，避免引入数据库依赖。 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("xiaocheng_prefs", Context.MODE_PRIVATE)

    init {
        // v1.2 起引擎默认本地；升级时只重置一次，之后尊重用户自己的选择。
        if (!sp.getBoolean("engine_default_local_v2", false)) {
            sp.edit()
                .putBoolean("use_local_engine", true)
                .putBoolean("engine_default_local_v2", true)
                .apply()
        }
    }

    var userName: String
        get() = sp.getString("user_name", "") ?: ""
        set(value) { sp.edit().putString("user_name", value).apply() }

    var apiBase: String
        get() = sp.getString("api_base", "https://api.deepseek.com/v1") ?: ""
        set(value) { sp.edit().putString("api_base", value).apply() }

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(value) { sp.edit().putString("api_key", value).apply() }

    var apiModel: String
        get() = sp.getString("api_model", "deepseek-chat") ?: ""
        set(value) { sp.edit().putString("api_model", value).apply() }

    /** 接入 API 后仍可手动切回本地规则。 */
    var useLocalEngine: Boolean
        get() = sp.getBoolean("use_local_engine", true)
        set(value) { sp.edit().putBoolean("use_local_engine", value).apply() }

    /** classic（现有分栏）或 combined（综合对话）。 */
    var aiMode: String
        get() = sp.getString("ai_mode", "classic") ?: "classic"
        set(value) { sp.edit().putString("ai_mode", value).apply() }

    /** 综合版 AI 的常驻要求 / 人设卡；空字符串表示使用内置默认。 */
    var aiRequirements: String
        get() = sp.getString("ai_requirements", "") ?: ""
        set(value) { sp.edit().putString("ai_requirements", value).apply() }

    /** 综合版是否允许联网搜索。 */
    var webSearchEnabled: Boolean
        get() = sp.getBoolean("web_search_enabled", true)
        set(value) { sp.edit().putBoolean("web_search_enabled", value).apply() }

    /** 综合版 AI 是否直接执行日程修改。 */
    var autoApplyActions: Boolean
        get() = sp.getBoolean("auto_apply_actions", true)
        set(value) { sp.edit().putBoolean("auto_apply_actions", value).apply() }

    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(value) { sp.edit().putBoolean("seeded", value).apply() }

    fun loadEvents(): List<ScheduleEvent> {
        val raw = sp.getString("events", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { ScheduleEvent.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun saveEvents(events: List<ScheduleEvent>) {
        val arr = JSONArray()
        events.forEach { arr.put(it.toJson()) }
        sp.edit().putString("events", arr.toString()).apply()
    }

    /** 每个通知页保存一份条目指纹快照，用于手动刷新时对比新增。 */
    fun loadSnapshot(url: String): Set<String>? {
        val raw = sp.getString("snapshots", "{}") ?: "{}"
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (!obj.has(url)) return null
        val arr = obj.optJSONArray(url) ?: return null
        return (0 until arr.length()).map { arr.optString(it) }.toSet()
    }

    fun saveSnapshot(url: String, keys: Set<String>) {
        val raw = sp.getString("snapshots", "{}") ?: "{}"
        val obj = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        obj.put(url, JSONArray(keys.toList()))
        sp.edit().putString("snapshots", obj.toString()).apply()
    }

    fun clearAll() {
        sp.edit().clear().apply()
    }
}