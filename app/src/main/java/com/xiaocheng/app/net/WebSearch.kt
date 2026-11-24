package com.xiaocheng.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
)

/** 轻量网页搜索：Bing HTML 优先，DuckDuckGo HTML 兜底。不需要额外 API Key。 */
class WebSearch {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun search(query: String, limit: Int = 5): List<WebSearchResult> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank()) return@withContext emptyList()
            val encoded = URLEncoder.encode(q, "UTF-8")
            bing(encoded, limit, "www.bing.com")
                .ifEmpty { bing(encoded, limit, "cn.bing.com") }
                .ifEmpty { duck(encoded, limit) }
        }

    private fun bing(encoded: String, limit: Int, host: String): List<WebSearchResult> {
        return try {
            val html = get("https://" + host + "/search?q=" + encoded + "&setlang=zh-Hans")
            val doc = Jsoup.parse(html, "https://" + host + "/")
            val out = ArrayList<WebSearchResult>()
            for (el in doc.select("li.b_algo")) {
                val a = el.selectFirst("h2 a") ?: continue
                val title = a.text().trim()
                val href = a.absUrl("href").ifBlank { a.attr("href") }
                if (title.isBlank() || !href.startsWith("http")) continue
                val snippet = el.selectFirst(".b_caption p")?.text()?.trim()
                    ?: el.selectFirst(".b_snippet")?.text()?.trim().orEmpty()
                out.add(WebSearchResult(title, href, snippet))
                if (out.size >= limit) break
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun duck(encoded: String, limit: Int): List<WebSearchResult> {
        return try {
            val html = get("https://html.duckduckgo.com/html/?q=" + encoded)
            val doc = Jsoup.parse(html, "https://html.duckduckgo.com/")
            val out = ArrayList<WebSearchResult>()
            for (el in doc.select(".result")) {
                val a = el.selectFirst(".result__a") ?: continue
                val title = a.text().trim()
                var href = a.absUrl("href").ifBlank { a.attr("href") }
                if (href.contains("uddg=")) {
                    val raw = href.substringAfter("uddg=").substringBefore("&")
                    href = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(href)
                }
                if (title.isBlank() || !href.startsWith("http")) continue
                val snippet = el.selectFirst(".result__snippet")?.text()?.trim().orEmpty()
                out.add(WebSearchResult(title, href, snippet))
                if (out.size >= limit) break
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun get(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        return http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return ""
            resp.body?.string().orEmpty()
        }
    }
}