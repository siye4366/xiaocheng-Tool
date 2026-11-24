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

    suspend fun download(
        context: Context,
        url: String,
        version: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (url.isBlank()) throw IOException("没有下载地址")
        val safeVersion = version.replace(Regex("[^0-9.]"), "_").ifBlank { "latest" }
        val out = File(context.cacheDir, "xiaocheng-update-" + safeVersion + ".apk")
        context.cacheDir.listFiles()?.forEach { old ->
            if (old.name.startsWith("xiaocheng-update") && old.absolutePath != out.absolutePath) {
                runCatching { old.delete() }
            }
        }
        if (out.exists()) out.delete()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "XiaoCheng-Android")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("下载失败 " + resp.code)
            val body = resp.body ?: throw IOException("下载内容为空")
            val total = body.contentLength()
            var read = 0L
            var last = -1
            val buffer = ByteArray(8 * 1024)
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val p = ((read * 100) / total).toInt().coerceIn(0, 100)
                            if (p != last) {
                                last = p
                                onProgress(p)
                            }
                        }
                    }
                }
            }
            if (total <= 0) onProgress(100)
        }
        if (out.length() < 100_000) throw IOException("安装包下载不完整")
        out
    }
}