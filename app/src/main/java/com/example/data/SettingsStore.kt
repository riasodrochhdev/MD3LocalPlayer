package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SettingsState

/** Сохраняет настройки между запусками (раньше они сбрасывались при каждом открытии). */
class SettingsStore(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("mediateka_settings", Context.MODE_PRIVATE)

  fun load(): SettingsState {
    val d = SettingsState()
    return SettingsState(
      themeId = prefs.getString("themeId", d.themeId) ?: d.themeId,
      glassStyle = prefs.getString("glassStyle", d.glassStyle) ?: d.glassStyle,
      snowEnabled = prefs.getBoolean("snowEnabled", d.snowEnabled),
      snowAmount = prefs.getInt("snowAmount", d.snowAmount),
      favIcon = prefs.getString("favIcon", d.favIcon) ?: d.favIcon,
      vibrationEnabled = prefs.getBoolean("vibrationEnabled", d.vibrationEnabled),
      alwaysAnimations = prefs.getBoolean("alwaysAnimations", d.alwaysAnimations),
      bgType = prefs.getString("bgType", d.bgType) ?: d.bgType,
      bgColorByCover = prefs.getBoolean("bgColorByCover", d.bgColorByCover),
      bgMoveOnlyWhilePlaying = prefs.getBoolean("bgMoveOnlyWhilePlaying", d.bgMoveOnlyWhilePlaying),
      bgSpeed = prefs.getInt("bgSpeed", d.bgSpeed),
      bgIntensity = prefs.getInt("bgIntensity", d.bgIntensity),
      feedLanguage = prefs.getString("feedLanguage", d.feedLanguage) ?: d.feedLanguage,
      myArtists = readList("myArtists", d.myArtists),
      banArtists = readList("banArtists", d.banArtists),
      tracksPerArtist = prefs.getInt("tracksPerArtist", d.tracksPerArtist),
      feedSize = prefs.getInt("feedSize", d.feedSize),
      sourceTaste = prefs.getBoolean("sourceTaste", d.sourceTaste),
      sourceAudius = prefs.getBoolean("sourceAudius", d.sourceAudius),
      sourceYt = prefs.getBoolean("sourceYt", d.sourceYt),
      feedAutoPlay = prefs.getBoolean("feedAutoPlay", d.feedAutoPlay),
      feedAutoScroll = prefs.getBoolean("feedAutoScroll", d.feedAutoScroll),
      ymGateway = prefs.getString("ymGateway", d.ymGateway) ?: d.ymGateway,
      ymToken = prefs.getString("ymToken", d.ymToken) ?: d.ymToken,
      ymDeviceId = prefs.getString("ymDeviceId", d.ymDeviceId) ?: d.ymDeviceId,
      cobaltUrl = prefs.getString("cobaltUrl", d.cobaltUrl) ?: d.cobaltUrl,
      cobaltKey = prefs.getString("cobaltKey", d.cobaltKey) ?: d.cobaltKey,
      ytWorker = prefs.getString("ytWorker", d.ytWorker) ?: d.ytWorker,
      pipedUrl = prefs.getString("pipedUrl", d.pipedUrl) ?: d.pipedUrl,
      ytApiKey = prefs.getString("ytApiKey", d.ytApiKey) ?: d.ytApiKey,
      downloadDestination = prefs.getString("downloadDestination", d.downloadDestination) ?: d.downloadDestination
    )
  }

  fun save(s: SettingsState) {
    prefs.edit()
      .putString("themeId", s.themeId)
      .putString("glassStyle", s.glassStyle)
      .putBoolean("snowEnabled", s.snowEnabled)
      .putInt("snowAmount", s.snowAmount)
      .putString("favIcon", s.favIcon)
      .putBoolean("vibrationEnabled", s.vibrationEnabled)
      .putBoolean("alwaysAnimations", s.alwaysAnimations)
      .putString("bgType", s.bgType)
      .putBoolean("bgColorByCover", s.bgColorByCover)
      .putBoolean("bgMoveOnlyWhilePlaying", s.bgMoveOnlyWhilePlaying)
      .putInt("bgSpeed", s.bgSpeed)
      .putInt("bgIntensity", s.bgIntensity)
      .putString("feedLanguage", s.feedLanguage)
      .putString("myArtists", s.myArtists.joinToString(SEP))
      .putString("banArtists", s.banArtists.joinToString(SEP))
      .putInt("tracksPerArtist", s.tracksPerArtist)
      .putInt("feedSize", s.feedSize)
      .putBoolean("sourceTaste", s.sourceTaste)
      .putBoolean("sourceAudius", s.sourceAudius)
      .putBoolean("sourceYt", s.sourceYt)
      .putBoolean("feedAutoPlay", s.feedAutoPlay)
      .putBoolean("feedAutoScroll", s.feedAutoScroll)
      .putString("ymGateway", s.ymGateway)
      .putString("ymToken", s.ymToken)
      .putString("ymDeviceId", s.ymDeviceId)
      .putString("cobaltUrl", s.cobaltUrl)
      .putString("cobaltKey", s.cobaltKey)
      .putString("ytWorker", s.ytWorker)
      .putString("pipedUrl", s.pipedUrl)
      .putString("ytApiKey", s.ytApiKey)
      .putString("downloadDestination", s.downloadDestination)
      .apply()
  }

  // ---- «Вкус»: сколько раз слушали каждого исполнителя ----
  fun loadTaste(): Map<String, Int> {
    val raw = prefs.getString("taste", "") ?: ""
    if (raw.isEmpty()) return emptyMap()
    val out = LinkedHashMap<String, Int>()
    raw.split(SEP).forEach { line ->
      val idx = line.lastIndexOf('=')
      if (idx > 0) {
        val n = line.substring(idx + 1).toIntOrNull() ?: return@forEach
        out[line.substring(0, idx)] = n
      }
    }
    return out
  }

  fun addTaste(artist: String, weight: Int = 1) {
    if (artist.isBlank()) return
    val cur = LinkedHashMap(loadTaste())
    cur[artist] = (cur[artist] ?: 0) + weight
    // держим словарь небольшим
    val trimmed = cur.entries.sortedByDescending { it.value }.take(60)
    prefs.edit().putString("taste", trimmed.joinToString(SEP) { "${it.key}=${it.value}" }).apply()
  }

  fun resetTaste() {
    prefs.edit().remove("taste").remove("seen").apply()
  }

  // ---- Уже показанные в ленте треки, чтобы лента не повторялась ----
  fun loadSeen(): Set<Long> {
    val raw = prefs.getString("seen", "") ?: ""
    if (raw.isEmpty()) return emptySet()
    return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
  }

  fun saveSeen(ids: Collection<Long>) {
    prefs.edit().putString("seen", ids.toList().takeLast(200).joinToString(",")).apply()
  }

  // ---- Недавние поисковые запросы ----
  fun loadRecent(default: List<String>): List<String> = readList("recent", default)

  fun saveRecent(list: List<String>) {
    prefs.edit().putString("recent", list.take(12).joinToString(SEP)).apply()
  }

  // ---- Локальные треки, которые пользователь удалил из медиатеки (чтобы сканер не вернул их) ----
  fun loadHidden(): Set<Long> {
    val raw = prefs.getString("hidden", "") ?: ""
    if (raw.isEmpty()) return emptySet()
    return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
  }

  fun addHidden(id: Long) {
    val cur = loadHidden() + id
    prefs.edit().putString("hidden", cur.joinToString(",")).apply()
  }

  private fun readList(key: String, default: List<String>): List<String> {
    if (!prefs.contains(key)) return default
    val raw = prefs.getString(key, "") ?: ""
    return if (raw.isEmpty()) emptyList() else raw.split(SEP).filter { it.isNotBlank() }
  }

  private companion object {
    const val SEP = "\n"
  }
}
