package com.example.api

import com.example.model.SettingsState
import com.example.model.Source
import com.example.model.Track
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Данные для построения ленты рекомендаций. */
data class FeedInput(
  val settings: SettingsState,
  val favorites: List<Track>,
  val libraryIds: Set<Long>,
  val taste: Map<String, Int>,
  val seen: Set<Long>
)

private data class Scored(val track: Track, val pri: Boolean, val fav: Boolean) {
  val score: Int get() = (if (pri) 2 else 0) + (if (fav) 1 else 0)
}

object OnlineMusicRepository {
  private val DEFAULT_QUERIES = listOf(
    "русский рэп", "русский рок", "новая русская музыка", "поп хиты", "indie", "lofi hip hop", "phonk", "русский поп"
  )

  /**
   * Ищет сразу во всех подключённых каталогах. Каждый каталог отвечает со своей скоростью,
   * поэтому onUpdate вызывается по мере прихода результатов, а не в самом конце.
   */
  suspend fun search(query: String, s: SettingsState, onUpdate: (List<Track>) -> Unit) {
    val parts = arrayOfNulls<List<Track>>(3)
    val lock = Mutex()

    coroutineScope {
      suspend fun finish(index: Int, list: List<Track>) {
        lock.withLock {
          currentCoroutineContext().ensureActive()
          parts[index] = list
          onUpdate(parts.filterNotNull().flatten().distinctBy { it.id })
        }
      }

      if (s.ymEnabled) {
        launch { finish(0, safe<List<Track>>(emptyList()) { YandexApi.searchTracks(s, query).take(8) }) }
      }
      launch { finish(1, safe<List<Track>>(emptyList()) { AudiusApi.search(query, 10) }) }
      launch { finish(2, safe<List<Track>>(emptyList()) { YouTubeApi.search(query, s, 10) }) }
    }
  }

  private fun matches(a: String, b: String): Boolean {
    val x = norm(cleanArtist(a))
    val y = norm(cleanArtist(b))
    return x.isNotEmpty() && y.isNotEmpty() && (x.contains(y) || y.contains(x))
  }

  /**
   * Лента рекомендаций: похожее на избранное, свои исполнители и (если избранного мало) любимое по прослушиваниям.
   */
  suspend fun buildFeed(input: FeedInput): List<Track> = coroutineScope {
    val s = input.settings
    val n = s.feedSize.coerceAtLeast(5)
    val cap = s.tracksPerArtist.coerceAtLeast(1)
    val ban = s.banArtists.map { norm(it) }.toSet()

    val favArtists = input.favorites
      .map { cleanArtist(it.artist) }
      .filter { !isGenericArtist(it) && norm(it) !in ban }
      .distinct()

    val ruShare = if (input.favorites.isEmpty()) 0.7 else
      input.favorites.count { RU_REGEX.containsMatchIn(it.title + " " + it.artist) }.toDouble() / input.favorites.size
    val lang = when (s.feedLanguage) {
      "fav" -> if (ruShare >= 0.5) "pref" else "any"
      else -> s.feedLanguage
    }

    val jobs = ArrayList<Deferred<List<Scored>>>()
    fun job(block: suspend () -> List<Scored>) {
      jobs.add(async {
        withTimeoutOrNull(9500L) { safe<List<Scored>>(emptyList()) { block() } } ?: emptyList()
      })
    }

    fun seed(artist: String, fromFav: Boolean) {
      if (s.sourceAudius) {
        job { AudiusApi.search(artist, 15).map { Scored(it, matches(it.artist, artist), fromFav) } }
      }
      if (s.ymEnabled) {
        job { YandexApi.searchTracks(s, artist).map { Scored(it, matches(it.artist, artist), fromFav) } }
      }
      if (s.sourceYt) {
        job { YouTubeApi.search("$artist песни", s, 12).map { Scored(it, matches(it.artist, artist), fromFav) } }
      }
    }

    fun defaults() {
      val q = DEFAULT_QUERIES.shuffled().take(3)
      if (s.sourceAudius) {
        q.forEach { text -> job { AudiusApi.search(text, 15).map { Scored(it, false, false) } } }
        if (lang == "any") job { AudiusApi.trending(20).map { Scored(it, false, false) } }
      }
      if (s.ymEnabled) {
        job { YandexApi.searchTracks(s, q[0]).map { Scored(it, false, false) } }
      }
      if (s.sourceYt) {
        job { YouTubeApi.search(q[0], s, 12).map { Scored(it, false, false) } }
      }
    }

    // 1) похожее на избранное
    favArtists.shuffled().take(3).forEach { seed(it, true) }
    if (s.ymEnabled) {
      input.favorites.shuffled().take(2).forEach { t ->
        job {
          val id = if (t.source == Source.YANDEX) t.sourceId else
            YandexApi.searchTracks(s, "${cleanArtist(t.artist)} ${t.title}").firstOrNull { sameTrack(t, it) }?.sourceId
          if (id == null) emptyList() else YandexApi.similar(s, id).map { Scored(it, false, true) }
        }
      }
    }

    // 2) свои исполнители
    s.myArtists.filter { it.isNotBlank() && norm(it) !in ban }.shuffled().take(3).forEach { seed(it, false) }

    // 3) прослушивания: только если избранного мало
    if (s.sourceTaste && favArtists.size < 3) {
      input.taste.entries.sortedByDescending { it.value }
        .map { it.key }
        .filter { !isGenericArtist(it) && norm(it) !in ban }
        .take(4).shuffled().take(2)
        .forEach { seed(it, false) }
    }

    if (jobs.isEmpty()) defaults()

    var pool = jobs.awaitAll().flatten()

    fun filter(list: List<Scored>): List<Scored> {
      val m = LinkedHashMap<String, Scored>()
      for (x in list) {
        val t = x.track
        if (t.id in input.libraryIds) continue
        if (norm(t.artist) in ban) continue
        if (lang == "ru" && !x.pri && !RU_REGEX.containsMatchIn(t.title + " " + t.artist)) continue
        val key = norm(cleanArtist(t.artist)) + "|" + norm(t.title)
        val old = m[key]
        if (old == null || (x.pri && !old.pri) || (x.fav && !old.fav)) m[key] = x
      }
      return m.values.toList()
    }

    var list = filter(pool)
    if (list.size < n / 2) {
      // источников не хватило: добавляем общие подборки
      jobs.clear()
      defaults()
      pool = pool + jobs.awaitAll().flatten()
      list = filter(pool)
    }

    val fresh = list.filter { it.track.id !in input.seen }
    if (fresh.size >= minOf(n, 8)) list = fresh

    if (lang == "pref") {
      val isRu = { x: Scored -> x.pri || x.fav || RU_REGEX.containsMatchIn(x.track.title + " " + x.track.artist) }
      list = list.filter(isRu) + list.filterNot(isRu).shuffled().take(maxOf(1, n / 8))
    }

    // не больше cap треков одного исполнителя; исполнители чередуются, лучшие совпадения идут первыми
    val groups = list
      .groupBy { norm(cleanArtist(it.track.artist)).ifEmpty { it.track.id.toString() } }
      .values
      .map { g -> g.sortedByDescending { it.score }.take(cap) }
    val out = ArrayList<Scored>()
    for (round in 0 until cap) {
      groups.shuffled()
        .sortedByDescending { it.firstOrNull()?.score ?: 0 }
        .forEach { g -> g.getOrNull(round)?.let { out.add(it) } }
    }
    out.map { it.track }.take(n).shuffled()
  }
}
