package org.littlelantern.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** Downloads a stream in 4 MB range requests, reporting progress (0 to 1, or -1 if unknown). */
fun downloadInChunks(client: OkHttpClient, url: String, target: File, onProgress: (Float) -> Unit) {
    val chunk = 4L * 1024 * 1024
    var position = 0L
    var total: Long? = null
    target.outputStream().use { out ->
        while (true) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=$position-${position + chunk - 1}")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 416) return
                if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")
                if (total == null) {
                    total = response.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                }
                val bytes = response.body?.bytes() ?: ByteArray(0)
                out.write(bytes)
                position += bytes.size
                val knownTotal = total
                onProgress(if (knownTotal != null && knownTotal > 0) position.toFloat() / knownTotal else -1f)
                val done = response.code == 200 || bytes.size < chunk || (knownTotal != null && position >= knownTotal)
                if (done) return
            }
        }
    }
}
