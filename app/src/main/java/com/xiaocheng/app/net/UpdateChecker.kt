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
    val apkUrl: String = "",
)

class UpdateChecker {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            checkApi(currentVersion)
        } catch (e: Exception) {
            checkWeb(currentVersion) ?: throw e
        }
    }

    private fun checkApi(currentVersion: String): UpdateInfo? {
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
        if (compare(tag, currentVersion) <= 0) return null
        var apkUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                if (a.optString("name").endsWith(".apk")) {
                    apkUrl = a.optString("browser_download_url")
                    break
                }
            }
        }
        return UpdateInfo(
            version = tag,
            url = json.optString("html_url").ifBlank { "https://github.com/siye4366/xiaocheng-Tool/releases" },
            notes = json.optString("body").take(4000),
            apkUrl = apkUrl,
        )
    }

    /** API 被代理/VPN 拦截时，退回到 GitHub Release 网页重定向。 */
    private fun checkWeb(currentVersion: String): UpdateInfo? {
        val req = Request.Builder()
            .url("https://github.com/siye4366/xiaocheng-Tool/releases/latest")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("网页返回 " + resp.code)
            val finalUrl = resp.request.url.toString()
            val tag = Regex("""/releases/tag/v?([^/?#]+)""").find(finalUrl)?.groupValues?.getOrNull(1)
                ?: throw IOException("没有找到版本标签")
            val version = tag.trim()
            if (version.isBlank() || compare(version, currentVersion) <= 0) return null
            return UpdateInfo(
                version = version,
                url = "https://github.com/siye4366/xiaocheng-Tool/releases/tag/v" + version,
                notes = "",
                apkUrl = "https://github.com/siye4366/xiaocheng-Tool/releases/download/v" + version +
                    "/xiaocheng-v" + version + ".apk",
            )
        }
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