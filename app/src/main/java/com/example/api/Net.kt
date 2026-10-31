package com.example.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Net {
  const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

  val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()
}

class HttpStatusException(val code: Int, val body: String) : IOException("HTTP $code")

class HttpResult(val code: Int, val body: String) {
  val ok: Boolean get() = code in 200..299
}

/** Корутинная обёртка над OkHttp: при отмене корутины запрос реально обрывается. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
  cont.invokeOnCancellation {
    try {
      cancel()
    } catch (_: Throwable) {
    }
  }
  enqueue(object : Callback {
    override fun onFailure(call: Call, e: IOException) {
      if (cont.isActive) cont.resumeWithException(e)
    }

    override fun onResponse(call: Call, response: Response) {
      if (cont.isActive) cont.resume(response) else response.close()
    }
  })
}

private suspend fun execute(request: Request, timeoutMs: Long): HttpResult {
  val client = Net.client.newBuilder()
    .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
    .build()
  return client.newCall(request).await().use { resp ->
    val text = withContext(Dispatchers.IO) { resp.body?.string().orEmpty() }
    HttpResult(resp.code, text)
  }
}

/** GET без исключения на HTTP-ошибки (нужно там, где в теле ошибки лежит полезный JSON). */
suspend fun httpGetRaw(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Long = 8000L): HttpResult {
  val rb = Request.Builder().url(url).header("User-Agent", Net.UA)
  headers.forEach { (k, v) -> rb.header(k, v) }
  return execute(rb.build(), timeoutMs)
}

suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Long = 8000L): String {
  val r = httpGetRaw(url, headers, timeoutMs)
  if (!r.ok) throw HttpStatusException(r.code, r.body)
  return r.body
}

suspend fun httpPostJson(url: String, json: String, headers: Map<String, String> = emptyMap(), timeoutMs: Long = 10000L): String {
  val rb = Request.Builder()
    .url(url)
    .header("User-Agent", Net.UA)
    .post(json.toRequestBody("application/json".toMediaType()))
  headers.forEach { (k, v) -> rb.header(k, v) }
  val r = execute(rb.build(), timeoutMs)
  if (!r.ok) throw HttpStatusException(r.code, r.body)
  return r.body
}

/** Выполняет блок и возвращает default при любой ошибке, но не глотает отмену корутины. */
inline fun <T> safe(default: T, block: () -> T): T {
  return try {
    block()
  } catch (e: CancellationException) {
    throw e
  } catch (_: Throwable) {
    default
  }
}

/** Запускает все задачи параллельно и возвращает первый непустой результат (аналог Promise.any). */
suspend fun <T : Any> firstSuccess(tasks: List<suspend () -> T?>): T? {
  if (tasks.isEmpty()) return null
  return coroutineScope {
    val channel = Channel<T?>(Channel.UNLIMITED)
    val jobs = tasks.map { task ->
      launch {
        val r: T? = try {
          task()
        } catch (e: CancellationException) {
          throw e
        } catch (_: Throwable) {
          null
        }
        channel.trySend(r)
      }
    }
    var result: T? = null
    for (i in tasks.indices) {
      val r = channel.receive()
      if (r != null) {
        result = r
        break
      }
    }
    jobs.forEach { it.cancel() }
    result
  }
}

fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

private val NORM_REGEX = Regex("[^\\p{L}\\p{N}]+")
private val WORD_REGEX = Regex("[^\\p{L}\\p{N}]+")
val RU_REGEX = Regex("[А-Яа-яЁё]")
private val GENERIC_ARTIST = Regex("^(Неизвестный исполнитель|YouTube|YouTube Music|Audius|Яндекс Музыка|<unknown>)$")

/** Сравнение имён без регистра, пробелов и знаков. */
fun norm(s: String): String = s.lowercase().replace(NORM_REGEX, "")

fun isGenericArtist(a: String): Boolean = a.isBlank() || GENERIC_ARTIST.matches(a.trim())

fun cleanArtist(a: String): String = a.removeSuffix(" - Topic").trim()

/** Приводит введённый адрес к виду https://host без хвостовых слэшей. Пустая строка остаётся пустой. */
fun cleanBase(url: String): String {
  val t = url.trim().trimEnd('/')
  if (t.isEmpty()) return ""
  return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
}

/** Похожи ли два трека (по названию и исполнителю), чтобы найти тот же трек в другом каталоге. */
fun sameTrack(a: com.example.model.Track, b: com.example.model.Track): Boolean {
  val words = a.title.lowercase().split(WORD_REGEX).filter { it.length > 2 }
  val t = b.title.lowercase()
  val hits = words.count { t.contains(it) }
  val na = norm(cleanArtist(a.artist))
  val nb = norm(cleanArtist(b.artist))
  val titleOk = if (words.isEmpty()) norm(a.title) == norm(b.title) else hits >= maxOf(1, (words.size + 1) / 2)
  val artistOk = na.isEmpty() || nb.isEmpty() || na == nb || na.contains(nb) || nb.contains(na)
  return titleOk && artistOk
}
