package com.example.model

/** Откуда трек: свой файл или онлайн-каталог. */
object Source {
  const val LOCAL = "local"
  const val AUDIUS = "au"
  const val YOUTUBE = "yt"
  const val YANDEX = "ym"
}

data class Track(
  val id: Long,
  val title: String,
  val artist: String,
  val album: String = "Медиатека",
  val durationSeconds: Int = 160,
  val coverHue: Int = 280,
  val coverUrl: String? = null,
  /** Готовая ссылка для проигрывания: content:// или file:// (локальные и скачанные треки). Для онлайн-треков пусто: ссылку ищем при нажатии. */
  val streamUrl: String? = null,
  val isFavorite: Boolean = false,
  val isDownloaded: Boolean = false,
  val source: String = Source.LOCAL,
  /** Идентификатор в онлайн-каталоге (id Audius, видео YouTube, трек Яндекса). */
  val sourceId: String? = null
) {
  val isOnline: Boolean
    get() = source != Source.LOCAL

  val formattedDuration: String
    get() {
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return "%d:%02d".format(minutes, seconds)
    }
}

data class Artist(
  val id: String,
  val name: String,
  val monthlyListeners: String = "124 тыс. слушателей в месяц",
  val tracksCount: Int = 18,
  val bio: String = "Российский исполнитель, музыкант и продюсер. Вайб, драйв и атмосферный бит.",
  val avatarUrl: String? = null
)

/** Стабильные Long-идентификаторы: один и тот же трек всегда получает один и тот же id. */
object Ids {
  fun of(source: String, sourceId: String): Long {
    var h = 1125899906842597L
    val s = "$source:$sourceId"
    for (c in s) {
      h = 31L * h + c.code.toLong()
    }
    // старший бит 2^41 гарантирует, что id не пересечётся с локальными (они маленькие)
    return (h and Long.MAX_VALUE) or (1L shl 41)
  }

  fun hue(text: String): Int = (text.hashCode() and 0x7FFFFFFF) % 360
}
