package com.xiaocheng.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** 从公开网站的 HTTP Date 头取网络时间，避免手机系统时间不准。 */
class NetworkTime {

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun now(): LocalDateTime? = withContext(Dispatchers.IO) {
        val urls = listOf("https://www.bing.com/", "https://github.com/")
        for (url in urls) {
            try {
                val req = Request.Builder().url(url).head()
                    .header("User-Agent", "XiaoCheng-Android")
                    .build()
                http.newCall(req).execute().use { resp ->
                    val raw = resp.header("Date") ?: return@use
                    val zoned = ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
                    return@withContext LocalDateTime.ofInstant(zoned.toInstant(), ZoneId.systemDefault())
                }
            } catch (e: Exception) {
                // try next
            }
        }
        null
    }
}
