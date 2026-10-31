package com.example.model

object TestData {
  val initialTracks = listOf(
    Track(
      id = 1L,
      title = "Вайбкод",
      artist = "Ржавый бит",
      album = "Вайбкод",
      durationSeconds = 160,
      coverHue = 295,
      isFavorite = true,
      isDownloaded = true
    ),
    Track(
      id = 2L,
      title = "teeth - slowed",
      artist = "blair",
      album = "slowed collection",
      durationSeconds = 151,
      coverHue = 240,
      isFavorite = true,
      isDownloaded = false
    ),
    Track(
      id = 3L,
      title = "Дофамин",
      artist = "YungMe...",
      album = "Дофамин",
      durationSeconds = 101,
      coverHue = 210,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 4L,
      title = "Дофамин (Acoustic)",
      artist = "YungMe...",
      album = "Дофамин",
      durationSeconds = 90,
      coverHue = 180,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 5L,
      title = "Белый 500",
      artist = "Нурминский",
      album = "Белый 500",
      durationSeconds = 204,
      coverHue = 270,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 6L,
      title = "Подъебал",
      artist = "Слепой",
      album = "Подъебал",
      durationSeconds = 198,
      coverHue = 330,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 7L,
      title = "Protoje - Who Know",
      artist = "Middle Eye",
      album = "Dub Collection",
      durationSeconds = 195,
      coverHue = 45,
      isFavorite = true,
      isDownloaded = false
    ),
    Track(
      id = 8L,
      title = "Quasimotion - Kevin",
      artist = "Middle Eye",
      album = "Dub Collection",
      durationSeconds = 175,
      coverHue = 120,
      isFavorite = true,
      isDownloaded = false
    ),
    Track(
      id = 9L,
      title = "Spirit & Flow",
      artist = "Middle Eye",
      album = "Dub Collection",
      durationSeconds = 225,
      coverHue = 35,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 10L,
      title = "Назад в кресты",
      artist = "Нурминский",
      album = "Пацанский",
      durationSeconds = 185,
      coverHue = 280,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 11L,
      title = "Пошел ты нахуй старый год",
      artist = "Ржавый бит",
      album = "Новогодний",
      durationSeconds = 132,
      coverHue = 300,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 12L,
      title = "Яйца",
      artist = "YungMe...",
      album = "Сингл",
      durationSeconds = 118,
      coverHue = 60,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 13L,
      title = "Под новый год",
      artist = "blair",
      album = "Зима",
      durationSeconds = 165,
      coverHue = 200,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 14L,
      title = "onda andar",
      artist = "onda andar",
      album = "onda andar",
      durationSeconds = 142,
      coverHue = 150,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 15L,
      title = "wifiskeleton - bones",
      artist = "wifiskeleton",
      album = "bones",
      durationSeconds = 155,
      coverHue = 260,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 16L,
      title = "ermachka0 - intro",
      artist = "ermachka0",
      album = "intro",
      durationSeconds = 120,
      coverHue = 310,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 17L,
      title = "Ночной город",
      artist = "Ржавый бит",
      album = "Вайбкод",
      durationSeconds = 190,
      coverHue = 285,
      isFavorite = false,
      isDownloaded = false
    ),
    Track(
      id = 18L,
      title = "Весна",
      artist = "blair",
      album = "slowed collection",
      durationSeconds = 170,
      coverHue = 160,
      isFavorite = false,
      isDownloaded = false
    )
  )

  val recommendations = listOf(
    Track(
      id = 101L,
      title = "Protoje-Who Kno...",
      artist = "Middle Eye",
      album = "Middle Eye",
      durationSeconds = 195,
      coverHue = 50
    ),
    Track(
      id = 102L,
      title = "Quasimotion-Kevin...",
      artist = "Middle Eye",
      album = "Middle Eye",
      durationSeconds = 175,
      coverHue = 110
    ),
    Track(
      id = 103L,
      title = "Spirit & Life",
      artist = "Middle Eye",
      album = "Middle Eye",
      durationSeconds = 225,
      coverHue = 35
    ),
    Track(
      id = 104L,
      title = "vers...",
      artist = "Flow...",
      album = "Flow",
      durationSeconds = 148,
      coverHue = 340
    )
  )

  val recentQueries = listOf(
    "назад в кресты",
    "пошел ты нахуй старый год",
    "яйца",
    "под новый год",
    "подъебал",
    "нурминский"
  )

  val sampleArtist = Artist(
    id = "rust_beat",
    name = "Ржавый бит",
    monthlyListeners = "124 тыс. слушателей в месяц",
    tracksCount = 18,
    bio = "Российский исполнитель и продюсер. Мрачный атмосферный звук, винтажные сэмплы, плотный бас и честная уличная лирика."
  )
}
