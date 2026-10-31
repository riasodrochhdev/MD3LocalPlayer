package com.example.api

import com.example.model.Ids
import com.example.model.SettingsState
import com.example.model.Source
import com.example.model.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * YouTube Music без официального API: публичные серверы Piped, свой воркер,
 * ключ YouTube Data API и Cobalt (для получения аудио).
 */
object YouTubeApi {
  val PUBLIC_PIPED = listOf(
    "https://pipedapi.kavin.rocks",
    "https://pipedapi.leptons.xyz",
    "https://pipedapi.nosebs.ru",
    "https://pipedapi.adminforge.de",
    "https://api.piped.yt",
    "https://pipedapi.reallyaweso.me",
    "https://api.piped.private.coffee",
    "https://pipedapi.darkness.services",
    "https://pipedapi.orangenet.cc",
    "https://pipedapi.drgns.space",
    "https://pipedapi.owo.si",
    "https://pipedapi.ducks.party"
  )

  private val VIDEO_ID = Regex("[?&]v=([\\w-]{6,})")

  private fun pipedBases(s: SettingsState): List<String> {
    val own = cleanBase(s.pipedUrl)
    return if (own.isNotEmpty()) listOf(own) + PUBLIC_PIPED else PUBLIC_PIPED
  }

  fun videoTrack(id: String, title: String, channel: String, durationSec: Int): Track {
    val artist = cleanArtist(channel).ifBlank { "YouTube" }
    val name = title.ifBlank { "YouTube Track" }
    return Track(
      id = Ids.of(Source.YOUTUBE, id),
      title = name,
      artist = artist,
      album = "YouTube Music",
      durationSeconds = durationSec.coerceAtLeast(0),
      coverHue = Ids.hue(name + artist),
      coverUrl = "https://i.ytimg.com/vi/$id/mqdefault.jpg",
      source = Source.YOUTUBE,
      sourceId = id
    )
  }

  // ---------------------------------------------------------------- поиск

  suspend fun search(query: String, s: SettingsState, limit: Int = 12): List<Track> {
    // 1) свой воркер: GET {адрес}/?q=... -> [{id, t, c, d}]
    val worker = cleanBase(s.ytWorker)
    if (worker.isNotEmpty()) {
      val r = safe<List<Track>>(emptyList()) {
        val arr = JSONArray(httpGet("$worker/?q=${enc(query)}", timeoutMs = 6000L))
        val list = ArrayList<Track>()
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val id = o.optString("id")
          if (id.isEmpty()) continue
          list.add(videoTrack(id, o.optString("t"), o.optString("c"), o.optInt("d", 0)))
        }
        list
      }
      if (r.isNotEmpty()) return r.take(limit)
    }

    // 2) публичные серверы Piped (все сразу, берём самый быстрый ответ)
    val piped = firstSuccess(pipedBases(s).map { base -> suspend { pipedSearchOne(base, query) } })
    if (piped != null) return piped.take(limit)

    // 3) официальный YouTube Data API, если задан ключ
    val key = s.ytApiKey.trim()
    if (key.isNotEmpty()) {
      return safe<List<Track>>(emptyList()) {
        val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10" +
          "&maxResults=$limit&q=${enc(query)}&key=${enc(key)}"
        val items = JSONObject(httpGet(url, timeoutMs = 8000L)).optJSONArray("items")
        val list = ArrayList<Track>()
        if (items != null) {
          for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            val id = o.optJSONObject("id")?.optString("videoId").orEmpty()
            if (id.isEmpty()) continue
            val sn = o.optJSONObject("snippet")
            list.add(videoTrack(id, unescape(sn?.optString("title").orEmpty()), sn?.optString("channelTitle").orEmpty(), 0))
          }
        }
        list
      }
    }
    return emptyList()
  }

  private suspend fun pipedSearchOne(base: String, query: String): List<Track>? {
    val j = JSONObject(httpGet("$base/search?filter=music_songs&q=${enc(query)}", timeoutMs = 4500L))
    val items = j.optJSONArray("items") ?: return null
    val list = ArrayList<Track>()
    for (i in 0 until items.length()) {
      val o = items.optJSONObject(i) ?: continue
      val id = VIDEO_ID.find(o.optString("url"))?.groupValues?.get(1) ?: continue
      list.add(videoTrack(id, o.optString("title"), o.optString("uploaderName"), o.optInt("duration", 0)))
    }
    return if (list.isEmpty()) null else list
  }

  private fun unescape(s: String): String =
    s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")

  // ---------------------------------------------------------------- аудио

  /** Прямая ссылка на аудио через Piped. googlevideo-ссылки привязаны к IP сервера, поэтому идём через его proxyUrl. */
  suspend fun audioUrl(videoId: String, s: SettingsState): String? =
    firstSuccess(pipedBases(s).map { base -> suspend { pipedAudioOne(base, videoId) } })

  private suspend fun pipedAudioOne(base: String, id: String): String? {
    val j = JSONObject(httpGet("$base/streams/$id", timeoutMs = 7000L))
    val arr = j.optJSONArray("audioStreams") ?: return null
    var best: String? = null
    var bestScore = -1L
    for (i in 0 until arr.length()) {
      val o = arr.optJSONObject(i) ?: continue
      val url = o.optString("url")
      if (url.isEmpty() || o.optBoolean("videoOnly", false)) continue
      val mime = o.optString("mimeType")
      if (mime.contains("video")) continue
      val prefersM4a = if (mime.contains("mp4") || mime.contains("m4a")) 1_000_000_000L else 0L
      val score = prefersM4a + o.optLong("bitrate", 0L)
      if (score > bestScore) {
        bestScore = score
        best = url
      }
    }
    val url = best ?: return null
    val proxy = j.optString("proxyUrl").trim().trimEnd('/')
    return if (proxy.isEmpty()) url else rewriteOrigin(url, proxy)
  }

  private fun rewriteOrigin(url: String, proxy: String): String {
    return try {
      val u = java.net.URI(url)
      val origin = u.scheme + "://" + u.rawAuthority
      url.replaceFirst(origin, proxy)
    } catch (_: Exception) {
      url
    }
  }

  /** Cobalt: свой сервер, отдаёт mp3 по ссылке на видео. */
  suspend fun cobaltAudio(videoId: String, s: SettingsState): String? {
    val base = cleanBase(s.cobaltUrl)
    if (base.isEmpty()) return null
    val headers = HashMap<String, String>()
    headers["Accept"] = "application/json"
    if (s.cobaltKey.isNotBlank()) headers["Authorization"] = "Api-Key ${s.cobaltKey.trim()}"
    val body = JSONObject()
      .put("url", "https://www.youtube.com/watch?v=$videoId")
      .put("downloadMode", "audio")
      .put("audioFormat", "mp3")
      .put("audioBitrate", "192")
      .toString()
    val j = JSONObject(httpPostJson("$base/", body, headers, 15000L))
    val url = j.optString("url")
    val status = j.optString("status")
    return if (url.isNotEmpty() && Regex("tunnel|redirect|stream").containsMatchIn(status)) url else null
  }
}
