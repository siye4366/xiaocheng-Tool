package com.xiaocheng.app.net

import com.xiaocheng.app.data.NoticeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Article(val title: String, val text: String, val url: String)

/** 只抓取用户主动提交的公开页面，不登录、不批量爬取。 */
class PageFetcher {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val dateHintRegex = Regex("""(20[0-9]{2}[-./年][0-9]{1,2}[-./月][0-9]{1,2}日?|[0-9]{1,2}[-./][0-9]{1,2})""")

    suspend fun fetchArticle(rawUrl: String): Article = withContext(Dispatchers.IO) {
        val url = normalize(rawUrl)
        val html = fetch(url)
        val doc = Jsoup.parse(html, url)
        val wechat = url.contains("mp.weixin.qq.com")
        val rawTitle = when {
            wechat -> doc.selectFirst("#activity-name")?.text().orEmpty()
            else -> ""
        }.ifBlank {
            doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()
        }.ifBlank { doc.title() }
        val title = cleanTitle(rawTitle)

        doc.select("script, style, noscript, svg, iframe, nav, footer, header").remove()

        val ordered = mutableListOf<Element>()
        if (wechat) {
            doc.selectFirst("#js_content")?.let { ordered.add(it) }
            doc.selectFirst(".rich_media_content")?.let { ordered.add(it) }
        }
        val selectors = listOf(
            "#vsb_content", ".v_news_content", ".content-con", ".article-content", ".article_content",
            "article", "main", "#content", ".news-content", ".article", ".content",
        )
        for (sel in selectors) {
            doc.selectFirst(sel)?.let { ordered.add(it) }
        }
        val chosen = ordered.firstOrNull { it.text().trim().length >= 120 }
            ?: readableBody(doc)
            ?: throw IOException("没有找到正文区域")

        val text = chosen.text().replace(Regex("""\s+"""), " ").trim()
        if (text.length < 60) throw IOException("正文太短，可能是动态加载或需要登录的页面")
        val publish = doc.selectFirst("#publish_time")?.text()?.trim().orEmpty()
        val header = if (publish.isBlank()) "" else "发布时间：" + publish + "\n"
        Article(title.ifBlank { "未命名文章" }, header + text.take(9000), url)
    }

    suspend fun fetchNoticeItems(rawUrl: String): List<NoticeItem> = withContext(Dispatchers.IO) {
        val url = normalize(rawUrl)
        val base = url.toHttpUrlOrNull() ?: throw IOException("链接格式不正确")
        val doc = Jsoup.parse(fetch(url), url)
        val navWords = setOf("首页", "上一页", "下一页", "尾页", "更多", "返回", "登录", "注册", "English", "网站地图")
        val items = doc.select("a[href]").mapNotNull { a ->
            val title = a.text().replace(Regex("""\s+"""), " ").trim()
            if (title.length !in 8..100 || title in navWords) return@mapNotNull null
            if (title.startsWith("首页") || title.startsWith("上一页") || title.startsWith("下一页")) return@mapNotNull null
            val abs = a.absUrl("href").ifBlank { a.attr("href") }
            if (!abs.startsWith("http")) return@mapNotNull null
            val target = abs.toHttpUrlOrNull() ?: return@mapNotNull null
            if (target.host != base.host) return@mapNotNull null
            if (abs.contains("javascript:", true)) return@mapNotNull null
            val parentText = a.parent()?.text().orEmpty()
            val dateHint = dateHintRegex.find(title)?.value
                ?: dateHintRegex.find(parentText)?.value ?: ""
            NoticeItem(title, abs, dateHint)
        }.distinctBy { it.url }.take(100)
        if (items.isEmpty()) throw IOException("没有识别到条目，页面可能是动态渲染的")
        items
    }

    /** 正文兜底：在所有 div/section 中选文字多、链接文字少的块，避开导航和页脚。 */
    private fun readableBody(doc: Document): Element? {
        val candidates = doc.select("div, article, section, main").toList().filter { it.text().trim().length >= 150 }
        return candidates.maxByOrNull { el ->
            val total = el.text().trim().length
            val linkLen = el.select("a").sumOf { it.text().trim().length }
            total - linkLen * 2
        } ?: doc.body()
    }

    private fun cleanTitle(raw: String): String {
        var value = raw.replace(Regex("""\s+"""), " ").trim()
            .split(Regex("""\s+[-_|｜]\s+""")).first().trim()
        val idx = value.lastIndexOf('-')
        if (idx > 8) {
            val tail = value.substring(idx + 1).trim()
            val siteLike = tail.endsWith("大学") || tail.endsWith("学院") ||
                tail.endsWith("网") || tail.endsWith("官网") || tail.endsWith("新闻网")
            if (tail.length in 2..14 && tail.none { it.isDigit() } && siteLike) {
                value = value.substring(0, idx).trim()
            }
        }
        return value.take(80)
    }

    private fun normalize(raw: String): String {
        val url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw IOException("请输入以 http:// 或 https:// 开头的链接")
        }
        return url
    }

    private fun fetch(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        return http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("页面返回 " + resp.code)
            resp.body?.string().orEmpty().ifBlank { throw IOException("页面内容为空") }
        }
    }
}