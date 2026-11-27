package com.xiaocheng.app.data

import org.json.JSONObject

/** 一个已保存的 API 配置，用于自动选择/轮换。 */
data class ApiProfile(
    val id: String,
    val name: String,
    val displayName: String = "",
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val lastUsed: Long = 0L,
    val cooldownUntil: Long = 0L,
    val lastError: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("displayName", displayName)
        put("baseUrl", baseUrl)
        put("model", model)
        put("apiKey", apiKey)
        put("lastUsed", lastUsed)
        put("cooldownUntil", cooldownUntil)
        put("lastError", lastError)
    }

    companion object {
        fun fromJson(o: JSONObject): ApiProfile = ApiProfile(
            id = o.optString("id"),
            name = o.optString("name"),
            displayName = o.optString("displayName"),
            baseUrl = o.optString("baseUrl"),
            model = o.optString("model"),
            apiKey = o.optString("apiKey"),
            lastUsed = o.optLong("lastUsed", 0L),
            cooldownUntil = o.optLong("cooldownUntil", 0L),
            lastError = o.optString("lastError"),
        )
    }
}