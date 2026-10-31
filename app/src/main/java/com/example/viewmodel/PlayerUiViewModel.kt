package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FeedInput
import com.example.api.OnlineMusicRepository
import com.example.api.StreamResolver
import com.example.api.YandexApi
import com.example.api.YmAccount
import com.example.api.YmException
import com.example.api.authPollOrNull
import com.example.api.cleanBase
import com.example.api.isGenericArtist
import com.example.data.SettingsStore
import com.example.db.MediatekaDatabase
import com.example.db.TrackRepository
import com.example.db.toEntity
import com.example.model.Artist
import com.example.model.SettingsState
import com.example.model.Source
import com.example.model.TestData
import com.example.model.Track
import com.example.player.AudioPlayerManager
import com.example.util.Downloader
import com.example.utils.MediaFileScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.abs

/** Код для входа в Яндекс: его нужно ввести на странице подтверждения. */
data class YmLoginState(val code: String, val url: String)

data class MediatekaUiState(
  val currentTab: String = "home", // "home", "search", "lib", "settings"
  val activeArtist: Artist? = null,
  val tracks: List<Track> = TestData.initialTracks,
  val recommendations: List<Track> = emptyList(),
  val isFeedLoading: Boolean = false,
  val onlineSearchResults: List<Track> = emptyList(),
  val isSearchingOnline: Boolean = false,
  val queue: List<Track> = emptyList(),
  val currentTrack: Track? = null,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val currentPositionSeconds: Int = 0,
  val currentVolume: Float = 1.0f,
  val isPlayerSheetOpen: Boolean = false,
  val coverShapeIndex: Int = 0,
  val isShuffle: Boolean = false,
  val repeatMode: Int = 0,
  val contextMenuTrack: Track? = null,
  val libraryFilter: String = "all",
  val searchQuery: String = "",
  val recentQueries: List<String> = TestData.recentQueries,
  val settings: SettingsState = SettingsState(),
  val settingsTab: String = "look",
  val toastMessage: String? = null,
  // Яндекс Музыка
  val ymAccount: YmAccount? = null,
  val ymError: String? = null,
  val ymLogin: YmLoginState? = null,
  // идущие загрузки
  val downloadingIds: Set<Long> = emptySet()
)

class PlayerUiViewModel(application: Application) : AndroidViewModel(application) {
  private val store = SettingsStore(application)
  private val playerManager = AudioPlayerManager(application)

  private val database = MediatekaDatabase.getDatabase(application)
  private val repository = TrackRepository(database.trackDao())

  private val _uiState = MutableStateFlow(
    MediatekaUiState(
      settings = store.load(),
      recentQueries = store.loadRecent(TestData.recentQueries)
    )
  )
  val uiState: StateFlow<MediatekaUiState> = _uiState.asStateFlow()

  @Volatile
  private var libraryById: Map<Long, Track> = emptyMap()

  private var playJob: Job? = null
  private var searchJob: Job? = null
  private var feedJob: Job? = null
  private var ymLoginJob: Job? = null

  /** Список, внутри которого работают «дальше/назад» (медиатека, лента или результаты поиска). */
  private var playContext: List<Track> = emptyList()
  private var contextIsFeed = false

  /** Какой по счёту способ получения звука сейчас играет (для перехода к следующему при ошибке). */
  private var playingStep = 0

  private var feedStarted = false
  private var feedStale = false

  init {
    playerManager.onEnded = { onTrackEnded() }
    playerManager.onError = { handlePlayerError() }

    viewModelScope.launch {
      repository.initDefaultTracksIfEmpty(TestData.initialTracks.map { it.toEntity() })

      launch {
        repository.allTracks.collect { entities ->
          onLibraryChanged(entities.map { it.toTrack() })
        }
      }

      launch {
        playerManager.playbackState.collect { ps ->
          _uiState.update { st ->
            var cur = st.currentTrack
            val realSec = (ps.durationMs / 1000L).toInt()
            if (cur != null && ps.trackId == cur.id && realSec > 0 && abs(cur.durationSeconds - realSec) > 2) {
              cur = cur.copy(durationSeconds = realSec)
            }
            st.copy(
              isPlaying = ps.isPlaying,
              isBuffering = ps.isBuffering,
              currentPositionSeconds = (ps.currentPositionMs / 1000L).toInt(),
              currentVolume = ps.volume,
              currentTrack = cur
            )
          }
        }
      }
    }

    scanLocalAudioFiles()
    if (_uiState.value.settings.ymEnabled) checkYm()
  }

  // ------------------------------------------------------------------ библиотека

  private fun onLibraryChanged(list: List<Track>) {
    libraryById = list.associateBy { it.id }
    _uiState.update { st ->
      val cur = st.currentTrack
      st.copy(
        tracks = list,
        currentTrack = if (cur == null) null else libraryById[cur.id] ?: cur,
        recommendations = decorate(st.recommendations),
        onlineSearchResults = decorate(st.onlineSearchResults)
      )
    }
    if (!feedStarted) {
      feedStarted = true
      refreshFeed()
    }
  }

  /** Подмешивает в онлайн-треки состояние из базы: избранное, загружено, путь к файлу. */
  private fun decorate(list: List<Track>): List<Track> {
    val map = libraryById
    if (map.isEmpty()) return list
    return list.map { t ->
      val db = map[t.id]
      if (db == null) t else t.copy(isFavorite = db.isFavorite, isDownloaded = db.isDownloaded, streamUrl = db.streamUrl)
    }
  }

  fun scanLocalAudioFiles() {
    viewModelScope.launch {
      val scanned = withContext(Dispatchers.IO) { MediaFileScanner.scanDeviceAudioFiles(getApplication()) }
      val hidden = store.loadHidden()
      val entities = scanned
        .filter { it.first.id !in hidden }
        .map { (track, uri) -> track.toEntity(uri.toString()) }
      if (entities.isNotEmpty()) repository.insertIfAbsent(entities)
    }
  }

  fun addPickedAudioFiles(uris: List<Uri>) {
    viewModelScope.launch {
      val app = getApplication<Application>()
      val entities = withContext(Dispatchers.IO) {
        val out = ArrayList<com.example.db.TrackEntity>()
        for (uri in uris) {
          try {
            if (DocumentsContract.isTreeUri(uri)) {
              try {
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
              } catch (_: Exception) {
              }
              MediaFileScanner.scanTree(app, uri).forEach { (t, u) -> out.add(t.toEntity(u.toString())) }
            } else {
              try {
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
              } catch (_: Exception) {
              }
              val (t, u) = MediaFileScanner.parseUriToTrack(app, uri)
              out.add(t.toEntity(u.toString()))
            }
          } catch (_: Exception) {
          }
        }
        out
      }
      if (entities.isNotEmpty()) {
        repository.insertIfAbsent(entities)
        showToast("Добавлено ${entities.size} треков")
      } else {
        showToast("Аудиофайлов не найдено")
      }
    }
  }

  // ------------------------------------------------------------------ навигация

  fun setTab(tab: String) {
    val prev = _uiState.value.currentTab
    _uiState.update { it.copy(currentTab = tab, activeArtist = null) }
    // вернулись из настроек: если менялись источники ленты, обновляем её
    if (prev == "settings" && tab != "settings" && feedStale) {
      feedStale = false
      refreshFeed()
    }
  }

  fun openArtist(name: String) {
    val artist = Artist(
      id = name.lowercase().replace(" ", "_"),
      name = name,
      monthlyListeners = if (name == "Ржавый бит") "124 тыс. слушателей в месяц" else "86 тыс. слушателей в месяц",
      tracksCount = _uiState.value.tracks.count { it.artist.contains(name, ignoreCase = true) }.coerceAtLeast(4)
    )
    _uiState.update { it.copy(activeArtist = artist) }
  }

  fun closeArtist() {
    _uiState.update { it.copy(activeArtist = null) }
  }

  // ------------------------------------------------------------------ воспроизведение

  /** Нажатие на трек в любом списке. */
  fun playTrack(track: Track) {
    val st = _uiState.value
    val ctx = when {
      st.currentTab == "search" && st.onlineSearchResults.any { it.id == track.id } -> st.onlineSearchResults
      st.recommendations.any { it.id == track.id } && st.tracks.none { it.id == track.id } -> st.recommendations
      else -> st.tracks
    }
    playContext = ctx
    contextIsFeed = ctx === st.recommendations
    if (st.currentTab == "search" && st.searchQuery.isNotBlank()) addRecentQuery(st.searchQuery.trim())
    startPlayback(track, 0)
  }

  private fun startPlayback(track: Track, startStep: Int) {
    playJob?.cancel()
    playJob = viewModelScope.launch {
      if (startStep == 0) {
        playerManager.pause()
        if (!isGenericArtist(track.artist)) store.addTaste(track.artist)
      }
      _uiState.update {
        it.copy(
          currentTrack = track,
          isBuffering = true,
          currentPositionSeconds = if (startStep == 0) 0 else it.currentPositionSeconds
        )
      }
      val settings = _uiState.value.settings
      val resolved = withContext(Dispatchers.Default) { StreamResolver.resolve(track, settings, startStep) }
      if (resolved == null) {
        _uiState.update { it.copy(isBuffering = false) }
        showToast(
          if (startStep == 0) "Не нашёл, чем проиграть «${track.title}»"
          else "Не удалось воспроизвести «${track.title}»"
        )
        return@launch
      }
      playingStep = resolved.step
      if (resolved.preview) showToast("Яндекс отдаёт только 30 секунд: для полного трека нужен Плюс")
      playerManager.play(track, resolved.url)
    }
  }

  /** Ссылка не открылась: пробуем следующий способ получения звука. */
  private fun handlePlayerError() {
    val track = _uiState.value.currentTrack ?: return
    startPlayback(track, playingStep + 1)
  }

  private fun onTrackEnded() {
    val st = _uiState.value
    if (st.repeatMode == 2) {
      playerManager.restart()
      return
    }
    advance(auto = true)
  }

  fun togglePlayPause() {
    val st = _uiState.value
    if (!playerManager.hasMedia) {
      val t = st.currentTrack ?: st.tracks.firstOrNull() ?: return
      playTrack(t)
      return
    }
    playerManager.togglePlayPause()
  }

  fun playNext(track: Track) {
    _uiState.update { state ->
      state.copy(queue = listOf(track) + state.queue)
    }
    showToast("Сыграет следующим")
  }

  fun addToQueue(track: Track) {
    _uiState.update { state ->
      state.copy(queue = state.queue + track)
    }
    showToast("Добавлено в очередь")
  }

  fun nextTrack() {
    advance(auto = false)
  }

  private fun advance(auto: Boolean) {
    val st = _uiState.value
    if (st.queue.isNotEmpty()) {
      val next = st.queue.first()
      _uiState.update { it.copy(queue = it.queue.drop(1)) }
      startPlayback(next, 0)
      return
    }
    val cur = st.currentTrack ?: return
    val ctx = if (playContext.isEmpty()) st.tracks else playContext
    if (ctx.isEmpty()) return
    // «Играть дальше по ленте» выключено: после трека из ленты тишина
    if (auto && contextIsFeed && !st.settings.feedAutoPlay) return

    val idx = ctx.indexOfFirst { it.id == cur.id }
    val next: Track? = when {
      st.isShuffle && ctx.size > 1 -> ctx.filter { it.id != cur.id }.random()
      idx in 0 until ctx.size - 1 -> ctx[idx + 1]
      !auto || st.repeatMode == 1 -> ctx.first()
      else -> null
    }
    if (next != null) startPlayback(next, 0)
  }

  fun previousTrack() {
    val st = _uiState.value
    val cur = st.currentTrack ?: return
    if (st.currentPositionSeconds > 3) {
      playerManager.seekToMs(0L)
      return
    }
    val ctx = if (playContext.isEmpty()) st.tracks else playContext
    if (ctx.isEmpty()) return
    val idx = ctx.indexOfFirst { it.id == cur.id }
    val prev = if (idx > 0) ctx[idx - 1] else ctx.last()
    startPlayback(prev, 0)
  }

  /** Кубик: включает перемешивание и играет случайный трек из медиатеки. */
  fun playRandom() {
    val st = _uiState.value
    val pool = st.tracks
    if (pool.isEmpty()) return
    _uiState.update { it.copy(isShuffle = true) }
    playContext = pool
    contextIsFeed = false
    startPlayback(pool.random(), 0)
  }

  /** «Мой микс»: рекомендации вперемешку с избранным. */
  fun playMix() {
    viewModelScope.launch {
      showToast("Собираю микс…")
      if (_uiState.value.recommendations.isEmpty()) refreshFeed().join()
      val st = _uiState.value
      var pool = (st.recommendations.shuffled().take(10) + st.tracks.filter { it.isFavorite }.shuffled().take(4)).shuffled()
      if (pool.isEmpty()) pool = st.tracks.shuffled()
      if (pool.isEmpty()) {
        showToast("Не получилось собрать микс: добавьте музыку или проверьте интернет")
        return@launch
      }
      playContext = pool
      contextIsFeed = false
      startPlayback(pool.first(), 0)
    }
  }

  fun seekTo(seconds: Int) {
    playerManager.seekToMs(seconds * 1000L)
  }

  fun setVolume(volume: Float) {
    playerManager.setVolume(volume)
  }

  fun toggleShuffle() {
    _uiState.update { it.copy(isShuffle = !it.isShuffle) }
  }

  fun cycleRepeat() {
    _uiState.update { it.copy(repeatMode = (it.repeatMode + 1) % 3) }
  }

  fun cycleCoverShape() {
    _uiState.update { it.copy(coverShapeIndex = (it.coverShapeIndex + 1) % 3) }
  }

  fun openPlayerSheet() {
    _uiState.update { it.copy(isPlayerSheetOpen = true) }
  }

  fun closePlayerSheet() {
    _uiState.update { it.copy(isPlayerSheetOpen = false) }
  }

  fun openContextMenu(track: Track) {
    _uiState.update { it.copy(contextMenuTrack = track) }
  }

  fun closeContextMenu() {
    _uiState.update { it.copy(contextMenuTrack = null) }
  }

  // ------------------------------------------------------------------ избранное, загрузки, удаление

  fun toggleFavorite(trackId: Long) {
    viewModelScope.launch {
      val st = _uiState.value
      val track = st.tracks.firstOrNull { it.id == trackId }
        ?: st.recommendations.firstOrNull { it.id == trackId }
        ?: st.onlineSearchResults.firstOrNull { it.id == trackId }
        ?: st.currentTrack?.takeIf { it.id == trackId }
        ?: return@launch
      val inDb = repository.getById(trackId) != null
      if (!inDb) {
        // онлайн-трек ещё не в медиатеке: сохраняем его, иначе лайк пропадёт после перезапуска
        repository.insertOrUpdate(track.copy(isFavorite = true).toEntity())
      } else if (track.isFavorite && track.isOnline && !track.isDownloaded) {
        // убрали лайк у онлайн-трека, который больше нигде не нужен
        repository.deleteTrack(trackId)
      } else {
        repository.toggleFavorite(trackId, !track.isFavorite)
      }
    }
  }

  fun downloadTrack(track: Track) {
    val st = _uiState.value
    if (track.source == Source.LOCAL && !track.streamUrl.isNullOrBlank() && !track.streamUrl.startsWith("file:")) {
      showToast("Этот трек уже на устройстве")
      return
    }
    if (track.isDownloaded && !track.streamUrl.isNullOrBlank()) {
      showToast("Уже в медиатеке, раздел «Загрузки»")
      return
    }
    if (track.id in st.downloadingIds) return

    _uiState.update { it.copy(downloadingIds = it.downloadingIds + track.id) }
    showToast("Готовлю загрузку…")

    viewModelScope.launch {
      val app = getApplication<Application>()
      val settings = _uiState.value.settings
      try {
        val res = withContext(Dispatchers.IO) {
          Downloader.download(app, track, settings) { pct -> if (pct == 50) showToast("Скачиваю: 50%") }
        }
        val dest = settings.downloadDestination
        val name = "${Downloader.safeName(track.artist)} - ${Downloader.safeName(track.title)}.${Downloader.extFor(res.mime)}"
        var exported = false
        if (dest != "lib") {
          exported = withContext(Dispatchers.IO) { Downloader.exportToDownloads(app, res.file, name, res.mime) }
        }
        val keepInLibrary = dest != "file" || !exported
        if (keepInLibrary) {
          val base = repository.getById(track.id)?.toTrack() ?: track
          repository.insertOrUpdate(base.copy(isDownloaded = true).toEntity(Uri.fromFile(res.file).toString()))
        } else {
          withContext(Dispatchers.IO) { res.file.delete() }
        }
        showToast(
          when {
            dest == "file" && exported -> "Файл сохранён в Загрузки/Mediateka"
            dest == "file" -> "Трек сохранён в медиатеке (в общие «Загрузки» можно с Android 10)"
            else -> "Скачано: трек в разделе «Загрузки», играет офлайн"
          }
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Downloader.PreviewOnlyException) {
        showToast("Яндекс отдаёт только 30 секунд этого трека: нужна подписка Плюс")
      } catch (_: Exception) {
        showToast(
          if (track.source == Source.YOUTUBE && !settings.ymEnabled)
            "Не удалось скачать с YouTube. Подключите Яндекс Музыку: трек скачается оттуда"
          else "Не удалось скачать трек"
        )
      } finally {
        _uiState.update { it.copy(downloadingIds = it.downloadingIds - track.id) }
      }
    }
  }

  fun deleteTrack(track: Track) {
    viewModelScope.launch {
      repository.deleteTrack(track.id)
      if (track.source == Source.LOCAL && track.streamUrl?.startsWith("content:") == true) store.addHidden(track.id)
      track.streamUrl?.takeIf { it.startsWith("file:") }?.let { u ->
        withContext(Dispatchers.IO) {
          try {
            Uri.parse(u).path?.let { File(it).delete() }
          } catch (_: Exception) {
          }
        }
      }
      showToast("Удалено из медиатеки")
    }
  }

  // ------------------------------------------------------------------ поиск

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    searchJob?.cancel()
    val q = query.trim()
    if (q.length < 2) {
      _uiState.update { it.copy(onlineSearchResults = emptyList(), isSearchingOnline = false) }
      return
    }
    _uiState.update { it.copy(isSearchingOnline = true) }
    searchJob = viewModelScope.launch {
      delay(350)
      val settings = _uiState.value.settings
      try {
        withContext(Dispatchers.Default) {
          OnlineMusicRepository.search(q, settings) { list ->
            _uiState.update { it.copy(onlineSearchResults = decorate(list)) }
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
      }
      _uiState.update { it.copy(isSearchingOnline = false) }
    }
  }

  private fun addRecentQuery(q: String) {
    val list = (listOf(q) + _uiState.value.recentQueries.filter { !it.equals(q, ignoreCase = true) }).take(12)
    _uiState.update { it.copy(recentQueries = list) }
    store.saveRecent(list)
  }

  fun clearRecentQueries() {
    _uiState.update { it.copy(recentQueries = emptyList()) }
    store.saveRecent(emptyList())
  }

  fun setLibraryFilter(filter: String) {
    _uiState.update { it.copy(libraryFilter = filter) }
  }

  // ------------------------------------------------------------------ лента

  fun refreshFeed(manual: Boolean = false): Job {
    feedJob?.cancel()
    val job = viewModelScope.launch {
      _uiState.update { it.copy(isFeedLoading = true) }
      val st = _uiState.value
      val input = FeedInput(
        settings = st.settings,
        favorites = st.tracks.filter { it.isFavorite },
        libraryIds = st.tracks.map { it.id }.toSet(),
        taste = store.loadTaste(),
        seen = store.loadSeen()
      )
      val list = try {
        withContext(Dispatchers.Default) { OnlineMusicRepository.buildFeed(input) }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        emptyList()
      }
      if (list.isNotEmpty()) store.saveSeen(input.seen + list.map { it.id })
      _uiState.update { it.copy(recommendations = decorate(list), isFeedLoading = false) }
      if (manual && list.isEmpty()) showToast("Лента пока пуста: проверьте интернет или источники в настройках")
    }
    feedJob = job
    return job
  }

  fun resetTaste() {
    store.resetTaste()
    showToast("Вкус сброшен")
    refreshFeed()
  }

  // ------------------------------------------------------------------ настройки

  private fun updateSettings(feedRelated: Boolean = false, transform: (SettingsState) -> SettingsState) {
    var saved: SettingsState? = null
    _uiState.update { st ->
      val n = transform(st.settings)
      saved = n
      st.copy(settings = n)
    }
    saved?.let { store.save(it) }
    if (feedRelated) feedStale = true
  }

  fun setSettingsTab(tab: String) {
    _uiState.update { it.copy(settingsTab = tab) }
  }

  fun setTheme(themeId: String) = updateSettings { it.copy(themeId = themeId) }
  fun setGlassStyle(style: String) = updateSettings { it.copy(glassStyle = style) }
  fun toggleSnow() = updateSettings { it.copy(snowEnabled = !it.snowEnabled) }
  fun setSnowAmount(amount: Int) = updateSettings { it.copy(snowAmount = amount) }
  fun setFavIcon(icon: String) = updateSettings { it.copy(favIcon = icon) }
  fun toggleVibration() = updateSettings { it.copy(vibrationEnabled = !it.vibrationEnabled) }
  fun toggleAnimations() = updateSettings { it.copy(alwaysAnimations = !it.alwaysAnimations) }
  fun setBgType(type: String) = updateSettings { it.copy(bgType = type) }
  fun toggleBgColorByCover() = updateSettings { it.copy(bgColorByCover = !it.bgColorByCover) }
  fun toggleBgMoveOnlyPlaying() = updateSettings { it.copy(bgMoveOnlyWhilePlaying = !it.bgMoveOnlyWhilePlaying) }
  fun setBgSpeed(speed: Int) = updateSettings { it.copy(bgSpeed = speed) }
  fun setBgIntensity(intensity: Int) = updateSettings { it.copy(bgIntensity = intensity) }
  fun setDownloadDestination(dest: String) = updateSettings { it.copy(downloadDestination = dest) }

  fun setFeedLanguage(lang: String) = updateSettings(feedRelated = true) { it.copy(feedLanguage = lang) }
  fun setTracksPerArtist(count: Int) = updateSettings(feedRelated = true) { it.copy(tracksPerArtist = count) }
  fun setFeedSize(size: Int) = updateSettings(feedRelated = true) { it.copy(feedSize = size) }
  fun toggleSourceTaste() = updateSettings(feedRelated = true) { it.copy(sourceTaste = !it.sourceTaste) }
  fun toggleSourceAudius() = updateSettings(feedRelated = true) { it.copy(sourceAudius = !it.sourceAudius) }
  fun toggleSourceYt() = updateSettings(feedRelated = true) { it.copy(sourceYt = !it.sourceYt) }
  fun toggleFeedAutoPlay() = updateSettings { it.copy(feedAutoPlay = !it.feedAutoPlay) }
  fun toggleFeedAutoScroll() = updateSettings { it.copy(feedAutoScroll = !it.feedAutoScroll) }

  fun addMyArtist(name: String) {
    val n = name.trim()
    if (n.isBlank()) return
    updateSettings(feedRelated = true) {
      if (it.myArtists.any { a -> a.equals(n, ignoreCase = true) }) it else it.copy(myArtists = it.myArtists + n)
    }
  }

  fun removeMyArtist(name: String) {
    updateSettings(feedRelated = true) { it.copy(myArtists = it.myArtists - name) }
  }

  // ---- сервисы: поиск и аудио с YouTube ----
  fun setCobaltUrl(v: String) = updateSettings { it.copy(cobaltUrl = cleanBase(v)) }
  fun setCobaltKey(v: String) = updateSettings { it.copy(cobaltKey = v.trim()) }
  fun setYtWorker(v: String) = updateSettings(feedRelated = true) { it.copy(ytWorker = cleanBase(v)) }
  fun setPipedUrl(v: String) = updateSettings(feedRelated = true) { it.copy(pipedUrl = cleanBase(v)) }
  fun setYtApiKey(v: String) = updateSettings(feedRelated = true) { it.copy(ytApiKey = v.trim()) }

  // ---- сервисы: Яндекс Музыка ----
  fun setYmGateway(v: String) {
    updateSettings(feedRelated = true) { it.copy(ymGateway = cleanBase(v)) }
    _uiState.update { it.copy(ymAccount = null, ymError = null) }
    checkYm()
  }

  fun setYmToken(v: String) {
    updateSettings(feedRelated = true) { it.copy(ymToken = v.trim()) }
    _uiState.update { it.copy(ymAccount = null, ymError = null) }
    checkYm()
    if (v.isNotBlank()) showToast("Токен сохранён")
  }

  fun signOutYm() {
    ymLoginJob?.cancel()
    updateSettings(feedRelated = true) { it.copy(ymToken = "") }
    _uiState.update { it.copy(ymAccount = null, ymError = null, ymLogin = null) }
    showToast("Вы вышли из Яндекс Музыки")
  }

  fun checkYm() {
    val s = _uiState.value.settings
    if (!s.ymEnabled) {
      _uiState.update { it.copy(ymAccount = null, ymError = null) }
      return
    }
    viewModelScope.launch {
      try {
        val acc = withContext(Dispatchers.Default) { YandexApi.status(s) }
        _uiState.update { it.copy(ymAccount = acc, ymError = null) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val msg = if (e is YmException && (e.status == 401 || e.status == 403)) {
          "Яндекс не принимает токен: войдите заново"
        } else {
          e.message ?: "шлюз не отвечает"
        }
        _uiState.update { it.copy(ymAccount = null, ymError = msg) }
      }
    }
  }

  fun startYmLogin() {
    val s0 = _uiState.value.settings
    if (s0.ymGateway.isBlank()) {
      showToast("Сначала укажите адрес шлюза")
      return
    }
    ymLoginJob?.cancel()
    ymLoginJob = viewModelScope.launch {
      var deviceId = s0.ymDeviceId
      if (deviceId.isBlank()) {
        deviceId = UUID.randomUUID().toString()
        updateSettings { it.copy(ymDeviceId = deviceId) }
      }
      val dc = try {
        withContext(Dispatchers.Default) { YandexApi.authStart(_uiState.value.settings, deviceId) }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        showToast("Шлюз не отвечает. Проверьте адрес Worker")
        return@launch
      }
      _uiState.update { it.copy(ymLogin = YmLoginState(dc.userCode, dc.url)) }

      val end = System.currentTimeMillis() + dc.expiresInSec * 1000L
      while (isActive && System.currentTimeMillis() < end) {
        delay(dc.intervalSec * 1000L)
        val poll = authPollOrNull(_uiState.value.settings, dc.deviceCode) ?: continue
        val token = poll.token
        if (token != null) {
          updateSettings(feedRelated = true) { it.copy(ymToken = token) }
          _uiState.update { it.copy(ymLogin = null) }
          showToast("Яндекс Музыка подключена")
          checkYm()
          return@launch
        }
        if (poll.error != null) {
          _uiState.update { it.copy(ymLogin = null) }
          showToast("Яндекс: ${poll.error}")
          return@launch
        }
      }
      _uiState.update { it.copy(ymLogin = null) }
      showToast("Код истёк. Попробуйте ещё раз")
    }
  }

  fun cancelYmLogin() {
    ymLoginJob?.cancel()
    _uiState.update { it.copy(ymLogin = null) }
  }

  // ------------------------------------------------------------------ служебное

  fun showToast(message: String) {
    _uiState.update { it.copy(toastMessage = message) }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  override fun onCleared() {
    playerManager.release()
    super.onCleared()
  }
}
