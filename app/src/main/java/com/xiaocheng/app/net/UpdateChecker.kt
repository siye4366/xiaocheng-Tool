package com.xiaocheng.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val url: String,
    val notes: String,
)

class UpdateChecker {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/siye4366/xiaocheng-Tool/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "XiaoCheng-Android")
            .build()
        val raw = http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("GitHub 返回 " + resp.code)
            body
        }
        val json = JSONObject(raw)
        val tag = json.optString("tag_name").removePrefix("v").trim()
        if (tag.isBlank()) throw IOException("没有找到版本信息")
        if (compare(tag, currentVersion) <= 0) return@withContext null
        UpdateInfo(
            version = tag,
            url = json.optString("html_url").ifBlank { "https://github.com/siye4366/xiaocheng-Tool/releases" },
            notes = json.optString("body").take(4000),
        )
    }

    private fun compare(a: String, b: String): Int {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
