package com.example.api

import com.example.model.Ids
import com.example.model.Source
import com.example.model.Track
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/** Audius: бесплатный каталог, играет напрямую без ключей. */
object AudiusApi {
  private const val APP = "MD3Player"
  private const val FALLBACK = "https://api.audius.co"

  @Volatile
  private var cachedHost: String? = null

  /** Audius раздаёт список рабочих узлов; берём случайный, при сбое откатываемся на общий адрес. */
  private suspend fun host(): String {
    cachedHost?.let { return it }
    val discovered = safe<String?>(null) {
      val arr = JSONObject(httpGet("https://api.audius.co", timeoutMs = 5000L)).optJSONArray("data")
      if (arr != null && arr.length() > 0) arr.optString((Math.random() * arr.length()).toInt()) else null
    }
    val chosen = (discovered?.takeIf { it.startsWith("http") } ?: FALLBACK).trimEnd('/')
    cachedHost = chosen
    return chosen
  }

  suspend fun streamUrl(id: String): String = "${host()}/v1/tracks/$id/stream?app_name=$APP"

  suspend fun search(query: String, limit: Int = 12): List<Track> =
    get("search?limit=$limit&query=${enc(query)}")

  suspend fun trending(limit: Int = 20): List<Track> =
    get("trending?time=week&limit=$limit")

  private suspend fun get(path: String): List<Track> {
    for (attempt in 0..1) {
      try {
        val glue = if (path.contains('?')) '&' else '?'
        val url = "${host()}/v1/tracks/$path${glue}app_name=$APP"
        val data = JSONObject(httpGet(url, timeoutMs = 8000L)).optJSONArray("data") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until data.length()) {
          val t = parse(data.optJSONObject(i))
          if (t != null) out.add(t)
        }
        return out
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // второй заход пойдёт через общий адрес
        cachedHost = FALLBACK
      }
    }
    return emptyList()
  }

  private fun parse(o: JSONObject?): Track? {
    if (o == null) return null
    val id = o.optString("id")
    if (id.isEmpty()) return null
    if (o.has("is_streamable") && !o.optBoolean("is_streamable", true)) return null
    val title = o.optString("title").ifBlank { "Без названия" }
    val artist = o.optJSONObject("user")?.optString("name")?.takeIf { it.isNotBlank() } ?: "Audius"
    val art = o.optJSONObject("artwork")
    val cover = art?.optString("480x480")?.takeIf { it.isNotBlank() }
      ?: art?.optString("150x150")?.takeIf { it.isNotBlank() }
    return Track(
      id = Ids.of(Source.AUDIUS, id),
      title = title,
      artist = artist,
      album = "Audius",
      durationSeconds = o.optInt("duration", 0),
      coverHue = Ids.hue(title + artist),
      coverUrl = cover,
      source = Source.AUDIUS,
      sourceId = id
    )
  }
}
