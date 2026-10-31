package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.api.Net
import com.example.api.StreamResolver
import com.example.api.await
import com.example.model.SettingsState
import com.example.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object Downloader {
  class PreviewOnlyException : IOException("preview")

  class Result(val file: File, val mime: String)

  /** Скачивает трек в папку приложения. Пробует способы по очереди, как и воспроизведение. */
  suspend fun download(
    context: Context,
    track: Track,
    s: SettingsState,
    onProgress: (Int) -> Unit
  ): Result {
    val dir = File(context.filesDir, "downloads")
    dir.mkdirs()
    var step = 0
    var lastError: Throwable? = null
    var sawPreview = false

    while (true) {
      val resolved = StreamResolver.resolve(track, s, step) ?: break
      step = resolved.step + 1
      if (resolved.preview) {
        // Яндекс без подписки отдаёт только 30 секунд: такие файлы не сохраняем
        sawPreview = true
        continue
      }
      try {
        val tmp = File(dir, "${track.id}.part")
        val mime = fetchToFile(resolved.url, tmp, onProgress)
        if (tmp.length() < 30_000L) {
          tmp.delete()
          throw IOException("файл слишком маленький")
        }
        val out = File(dir, "${track.id}.${extFor(mime)}")
        if (out.exists()) out.delete()
        if (!tmp.renameTo(out)) throw IOException("не удалось сохранить файл")
        return Result(out, mime)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Throwable) {
        lastError = e
      }
    }
    if (sawPreview) throw PreviewOnlyException()
    throw lastError ?: IOException("нет источника для скачивания")
  }

  private suspend fun fetchToFile(url: String, dest: File, onProgress: (Int) -> Unit): String {
    val client = Net.client.newBuilder().readTimeout(30, TimeUnit.SECONDS).build()
    val req = Request.Builder().url(url).header("User-Agent", Net.UA).build()
    return client.newCall(req).await().use { resp ->
      if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
      val body = resp.body ?: throw IOException("пустой ответ")
      val ct = resp.header("Content-Type").orEmpty()
      if (ct.contains("text") || ct.contains("json") || ct.contains("html")) throw IOException("это не аудио")
      val total = body.contentLength()
      withContext(Dispatchers.IO) {
        body.byteStream().use { input ->
          dest.outputStream().use { out ->
            val buf = ByteArray(64 * 1024)
            var done = 0L
            var lastStep = -1
            while (true) {
              val read = input.read(buf)
              if (read < 0) break
              out.write(buf, 0, read)
              done += read
              if (total > 0) {
                val stepPct = (done * 10 / total).toInt()
                if (stepPct != lastStep) {
                  lastStep = stepPct
                  onProgress(stepPct * 10)
                }
              }
              ensureActive()
            }
          }
        }
      }
      ct.substringBefore(';').trim()
    }
  }

  fun extFor(mime: String): String {
    val m = mime.lowercase()
    return when {
      m.contains("mpeg") || m.contains("mp3") -> "mp3"
      m.contains("mp4") || m.contains("m4a") || m.contains("aac") -> "m4a"
      m.contains("webm") -> "webm"
      m.contains("ogg") || m.contains("opus") -> "ogg"
      m.contains("flac") -> "flac"
      m.contains("wav") -> "wav"
      else -> "mp3"
    }
  }

  fun safeName(s: String): String = s.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(80)

  /** Копирует файл в общую папку «Загрузки/Mediateka» (Android 10+). */
  fun exportToDownloads(context: Context, file: File, displayName: String, mime: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
        put(MediaStore.Downloads.MIME_TYPE, mime.ifBlank { "audio/mpeg" })
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Mediateka")
      }
      val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
      val out = context.contentResolver.openOutputStream(uri) ?: return false
      out.use { o -> file.inputStream().use { it.copyTo(o) } }
      true
    } catch (_: Exception) {
      false
    }
  }
}
