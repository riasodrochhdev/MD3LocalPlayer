package com.example.api

import com.example.model.SettingsState
import com.example.model.Source
import com.example.model.Track

/** Найденная ссылка на звук. step нужен, чтобы при ошибке воспроизведения попробовать следующий способ. */
data class Resolved(val url: String, val step: Int, val preview: Boolean = false)

/**
 * Превращает трек в ссылку, которую умеет играть ExoPlayer.
 * Для каждого источника есть цепочка способов: если один не сработал, пробуется следующий.
 */
private typealias Step = suspend (Int) -> Resolved?

object StreamResolver {
  suspend fun resolve(track: Track, s: SettingsState, startStep: Int = 0): Resolved? {
    val steps = buildSteps(track, s)
    for (i in startStep until steps.size) {
      val r = safe<Resolved?>(null) { steps[i](i) }
      if (r != null) return r
    }
    return null
  }

  private fun buildSteps(track: Track, s: SettingsState): List<Step> {
    val steps = ArrayList<Step>()

    // 1) готовый файл: локальный, скачанный или выбранный через проводник
    val local = track.streamUrl
    if (!local.isNullOrBlank()) {
      steps.add { i -> Resolved(local, i) }
    }

    // 2) родной источник трека
    val id = track.sourceId
    if (id != null) {
      when (track.source) {
        Source.AUDIUS -> {
          steps.add { i -> Resolved(AudiusApi.streamUrl(id), i) }
        }
        Source.YANDEX -> {
          if (s.ymEnabled) {
            steps.add { i ->
              val f = YandexApi.file(s, id)
              Resolved(f.url, i, f.preview)
            }
            steps.add { i -> Resolved(YandexApi.gatewayStreamUrl(s, id), i) }
          }
        }
        Source.YOUTUBE -> {
          if (s.cobaltUrl.isNotBlank()) {
            steps.add { i -> YouTubeApi.cobaltAudio(id, s)?.let { Resolved(it, i) } }
          }
          steps.add { i -> YouTubeApi.audioUrl(id, s)?.let { Resolved(it, i) } }
        }
      }
    }

    // 3) запасные пути: найти тот же трек в других каталогах
    if (track.source != Source.AUDIUS) {
      steps.add { i ->
        val hit = AudiusApi.search("${cleanArtist(track.artist)} ${track.title}", 8)
          .firstOrNull { sameTrack(track, it) }
        hit?.sourceId?.let { Resolved(AudiusApi.streamUrl(it), i) }
      }
    }
    if (track.source != Source.YANDEX && s.ymEnabled) {
      steps.add { i ->
        val hit = YandexApi.searchTracks(s, "${cleanArtist(track.artist)} ${track.title}")
          .firstOrNull { sameTrack(track, it) }
        val hitId = hit?.sourceId
        if (hitId == null) {
          null
        } else {
          val f = YandexApi.file(s, hitId)
          Resolved(f.url, i, f.preview)
        }
      }
    }
    if (track.source == Source.LOCAL) {
      steps.add { i ->
        val hit = YouTubeApi.search("${cleanArtist(track.artist)} ${track.title}", s, 6)
          .firstOrNull { sameTrack(track, it) }
        val hitId = hit?.sourceId
        if (hitId == null) null else YouTubeApi.audioUrl(hitId, s)?.let { Resolved(it, i) }
      }
    }
    return steps
  }
}
