package com.example.api

import com.example.model.Ids
import com.example.model.SettingsState
import com.example.model.Source
import com.example.model.Track
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException

class YmException(val status: Int, message: String) : IOException(message)

data class YmFile(val url: String, val preview: Boolean, val codec: String)
data class YmAccount(val login: String, val plus: Boolean)
data class YmDeviceCode(val deviceCode: String, val userCode: String, val url: String, val intervalSec: Int, val expiresInSec: Int)
class YmPoll(val token: String?, val error: String?)

/**
 * Яндекс Музыка через ваш шлюз (Cloudflare Worker).
 * Шлюз проксирует неофициальный API, поэтому приложение само ничего не знает про подпись ссылок.
 *   GET {шлюз}/ym/api/<путь>        - прокси к api.music.yandex.net, токен в заголовке X-YM-Token
 *   GET {шлюз}/ym/file?id=          - {url, preview, codec}
 *   GET {шлюз}/ym/stream?id=&t=     - поток через шлюз
 *   GET {шлюз}/ym/auth/start?d=     - вход по коду
 *   GET {шлюз}/ym/auth/poll?code=   - ожидание подтверждения
 */
object YandexApi {
  private fun base(s: SettingsState) = cleanBase(s.ymGateway)
  private fun headers(s: SettingsState) = mapOf("X-YM-Token" to s.ymToken)

  private fun errorText(body: String): String? {
    return try {
      val j = JSONObject(body)
      val e = j.opt("error")
      when (e) {
        is JSONObject -> e.optString("message").ifEmpty { e.optString("name") }.ifEmpty { null }
        is String -> e.ifEmpty { null }
        else -> null
      }
    } catch (_: Exception) {
      null
    }
  }

  private suspend fun api(s: SettingsState, path: String): JSONObject {
    if (base(s).isEmpty()) throw IOException("Не указан адрес шлюза")
    val r = httpGetRaw("${base(s)}/ym/api/$path", headers(s), 10000L)
    if (!r.ok) throw YmException(r.code, errorText(r.body) ?: "Яндекс ${r.code}")
    val j = try {
      JSONObject(r.body)
    } catch (_: Exception) {
      throw YmException(r.code, "Шлюз ответил не JSON")
    }
    if (j.has("error") && !j.isNull("error")) throw YmException(r.code, errorText(r.body) ?: "Ошибка Яндекса")
    return j.optJSONObject("result") ?: JSONObject()
  }

  private fun coverUrl(uri: String?): String? {
    if (uri.isNullOrEmpty()) return null
    return "https://" + uri.removePrefix("https://").removePrefix("http://").replace("%%", "400x400")
  }

  private fun mapTrack(x: JSONObject?): Track? {
    if (x == null) return null
    val rawTitle = x.optString("title")
    if (rawTitle.isEmpty()) return null
    if (x.has("available") && !x.optBoolean("available", true)) return null
    val version = x.optString("version")
    val title = if (version.isNotEmpty()) "$rawTitle ($version)" else rawTitle
    val names = ArrayList<String>()
    val artists = x.optJSONArray("artists")
    if (artists != null) {
      for (i in 0 until artists.length()) {
        val n = artists.optJSONObject(i)?.optString("name").orEmpty()
        if (n.isNotEmpty()) names.add(n)
      }
    }
    val artist = if (names.isEmpty()) "Яндекс Музыка" else names.joinToString(", ")
    val id = x.opt("id")?.toString()?.substringBefore(':')?.takeIf { it.isNotEmpty() && it != "null" } ?: return null
    val coverRaw = x.optString("coverUri").ifEmpty {
      x.optJSONArray("albums")?.optJSONObject(0)?.optString("coverUri").orEmpty()
    }
    return Track(
      id = Ids.of(Source.YANDEX, id),
      title = title,
      artist = artist,
      album = "Яндекс Музыка",
      durationSeconds = x.optInt("durationMs", 0) / 1000,
      coverHue = Ids.hue(title + artist),
      coverUrl = coverUrl(coverRaw),
      source = Source.YANDEX,
      sourceId = id
    )
  }

  suspend fun searchTracks(s: SettingsState, query: String): List<Track> {
    val r = api(s, "search?text=${enc(query)}&nocorrect=false&type=track&page=0&playlist-in-best=true")
    val arr = r.optJSONObject("tracks")?.optJSONArray("results") ?: return emptyList()
    val out = ArrayList<Track>()
    for (i in 0 until arr.length()) {
      val t = mapTrack(arr.optJSONObject(i))
      if (t != null) out.add(t)
    }
    return out
  }

  suspend fun similar(s: SettingsState, trackId: String): List<Track> {
    val r = api(s, "tracks/$trackId/similar")
    val arr = r.optJSONArray("similarTracks") ?: return emptyList()
    val out = ArrayList<Track>()
    for (i in 0 until arr.length()) {
      val t = mapTrack(arr.optJSONObject(i))
      if (t != null) out.add(t)
    }
    return out
  }

  suspend fun file(s: SettingsState, id: String): YmFile {
    val r = httpGetRaw("${base(s)}/ym/file?id=${enc(id)}", headers(s), 12000L)
    val j = try {
      JSONObject(r.body)
    } catch (_: Exception) {
      throw IOException("Шлюз ответил не JSON")
    }
    if (!r.ok || (j.has("error") && !j.isNull("error"))) {
      throw IOException(j.optString("error").ifEmpty { "file ${r.code}" })
    }
    val url = j.optString("url")
    if (url.isEmpty()) throw IOException("Яндекс не дал ссылку")
    return YmFile(url, j.optBoolean("preview", false), j.optString("codec"))
  }

  fun gatewayStreamUrl(s: SettingsState, id: String): String =
    "${base(s)}/ym/stream?id=${enc(id)}&t=${enc(s.ymToken)}"

  suspend fun status(s: SettingsState): YmAccount {
    val r = api(s, "account/status")
    val acc = r.optJSONObject("account")
    val login = acc?.optString("login").orEmpty().ifEmpty { acc?.optString("displayName").orEmpty() }.ifEmpty { "аккаунт" }
    val plus = r.optJSONObject("plus")?.optBoolean("hasPlus", false) ?: false
    return YmAccount(login, plus)
  }

  // ---------------------------------------------------------------- вход по коду

  suspend fun authStart(s: SettingsState, deviceId: String): YmDeviceCode {
    if (base(s).isEmpty()) throw IOException("Не указан адрес шлюза")
    val j = JSONObject(httpGet("${base(s)}/ym/auth/start?d=${enc(deviceId)}", timeoutMs = 10000L))
    val device = j.optString("device_code")
    if (device.isEmpty()) throw IOException("Шлюз не выдал код")
    return YmDeviceCode(
      deviceCode = device,
      userCode = j.optString("user_code"),
      url = j.optString("verification_url").ifEmpty { "https://ya.ru/device" },
      intervalSec = maxOf(3, j.optInt("interval", 5)),
      expiresInSec = j.optInt("expires_in", 300)
    )
  }

  suspend fun authPoll(s: SettingsState, deviceCode: String): YmPoll {
    val r = httpGetRaw("${base(s)}/ym/auth/poll?code=${enc(deviceCode)}", timeoutMs = 10000L)
    val j = try {
      JSONObject(r.body)
    } catch (_: Exception) {
      return YmPoll(null, null)
    }
    val token = j.optString("access_token")
    if (token.isNotEmpty()) return YmPoll(token, null)
    val err = j.optString("error")
    if (err.isNotEmpty() && err != "authorization_pending") {
      return YmPoll(null, j.optString("error_description").ifEmpty { err })
    }
    return YmPoll(null, null)
  }
}

/** Опрос входа по коду: временные сбои сети не должны обрывать ожидание, поэтому при ошибке возвращаем null. */
suspend fun authPollOrNull(s: SettingsState, deviceCode: String): YmPoll? {
  return try {
    YandexApi.authPoll(s, deviceCode)
  } catch (e: CancellationException) {
    throw e
  } catch (_: Exception) {
    null
  }
}
