package com.archieapps.calendar.core.media

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.archieapps.calendar.core.net.Http
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object AvatarLoader {
    private const val TARGET_PX = 160

    private val cached = mutableMapOf<String, ImageBitmap>()

    private val client get() = Http.shared

    suspend fun load(context: Context, url: String?): ImageBitmap? {
        if (url.isNullOrBlank()) return null

        cached[url]?.let { return it }

        return withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "avatar_${url.hashCode()}.img")
            val bytes = readCached(file) ?: download(url)?.also { file.writeBytes(it) }

            bytes?.let { decode(it) }?.also { cached[url] = it }
        }
    }

    private fun readCached(file: File): ByteArray? =
        if (file.exists() && file.length() > 0) runCatching { file.readBytes() }.getOrNull() else null

    private fun download(url: String): ByteArray? = runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.isSuccessful) response.body?.bytes() else null
        }
    }.getOrNull()

    private fun decode(bytes: ByteArray): ImageBitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (largest / sample > TARGET_PX * 2) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }

        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }.getOrNull()
}
