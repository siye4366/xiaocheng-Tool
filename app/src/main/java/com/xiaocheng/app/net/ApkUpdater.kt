package com.xiaocheng.app.net

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApkUpdater {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun download(context: Context, url: String): File = withContext(Dispatchers.IO) {
        if (url.isBlank()) throw IOException("没有下载地址")
        val out = File(context.cacheDir, "xiaocheng-update.apk")
        if (out.exists()) out.delete()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "XiaoCheng-Android")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("下载失败 " + resp.code)
            val body = resp.body ?: throw IOException("下载内容为空")
            out.outputStream().use { stream -> body.byteStream().copyTo(stream) }
        }
        if (out.length() < 100_000) throw IOException("安装包下载不完整")
        out
    }
}
