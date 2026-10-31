package com.example.model

data class SettingsState(
  val themeId: String = "violet",
  val glassStyle: String = "blur", // "off", "blur", "liquid"
  val snowEnabled: Boolean = false,
  val snowAmount: Int = 2, // 1: Мало, 2: Средне, 3: Много
  val favIcon: String = "plus", // "plus", "heart"
  val vibrationEnabled: Boolean = true,
  val alwaysAnimations: Boolean = true,

  // Background
  val bgType: String = "fluid", // "off", "fluid", "waves"
  val bgColorByCover: Boolean = true,
  val bgMoveOnlyWhilePlaying: Boolean = false,
  val bgSpeed: Int = 3, // 1: Медленно, 2: Средне, 3: Быстро
  val bgIntensity: Int = 1, // 1: Мягко, 2: Средне, 3: Ярко

  // Feed
  val feedLanguage: String = "fav", // "fav", "any", "pref", "ru"
  val myArtists: List<String> = listOf("ermachka0", "wifiskeleton", "blair", "onda andar"),
  val banArtists: List<String> = emptyList(),
  val tracksPerArtist: Int = 3, // 1, 2, 3, 5
  val feedSize: Int = 30, // 10, 20, 30
  val sourceTaste: Boolean = true,
  val sourceAudius: Boolean = true,
  val sourceYt: Boolean = true,
  val feedAutoPlay: Boolean = true,
  val feedAutoScroll: Boolean = false,

  // Services
  /** Адрес вашего Cloudflare Worker для Яндекс Музыки. Пусто = Яндекс выключен. */
  val ymGateway: String = "",
  val ymToken: String = "",
  val ymDeviceId: String = "",
  val cobaltUrl: String = "",
  val cobaltKey: String = "",
  /** Свой воркер для поиска по YouTube (отвечает на GET /?q=… списком [{id,t,c,d}]). */
  val ytWorker: String = "",
  val pipedUrl: String = "",
  val ytApiKey: String = "",
  val downloadDestination: String = "both" // "both", "lib", "file"
) {
  /** Яндекс считается подключённым, когда есть и шлюз, и токен. */
  val ymEnabled: Boolean
    get() = ymGateway.isNotBlank() && ymToken.isNotBlank()
}
