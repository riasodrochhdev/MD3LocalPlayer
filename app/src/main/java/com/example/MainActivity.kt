package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AmbientBackground
import com.example.ui.components.BottomNavBar
import com.example.ui.components.MiniPlayer
import com.example.ui.components.PlayerBottomSheet
import com.example.ui.components.SnowOverlay
import com.example.ui.components.TrackContextMenu
import com.example.ui.screens.ArtistScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AllThemes
import com.example.ui.theme.LocalHapticsEnabled
import com.example.ui.theme.MediatekaTheme
import com.example.ui.theme.ThemeViolet
import com.example.viewmodel.PlayerUiViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MediatekaApp()
    }
  }
}

@Composable
fun MediatekaApp(viewModel: PlayerUiViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val activeTheme = remember(uiState.settings.themeId) {
    AllThemes.find { it.id == uiState.settings.themeId } ?: ThemeViolet
  }

  // «Анимации: как в системе»: если в системе анимации выключены, мы тоже их выключаем
  val systemAnimationsOn = remember {
    try {
      Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    } catch (_: Exception) {
      true
    }
  }
  val animationsEnabled = uiState.settings.alwaysAnimations || systemAnimationsOn

  // Разрешения: доступ к музыке на телефоне и уведомления плеера
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) {
    viewModel.scanLocalAudioFiles()
  }
  LaunchedEffect(Unit) {
    val wanted = if (Build.VERSION.SDK_INT >= 33) {
      listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
      listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val missing = wanted.filter {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
  }

  // Back handler for artist detail or sheet overlay
  BackHandler(enabled = uiState.isPlayerSheetOpen || uiState.activeArtist != null) {
    if (uiState.isPlayerSheetOpen) {
      viewModel.closePlayerSheet()
    } else if (uiState.activeArtist != null) {
      viewModel.closeArtist()
    }
  }

  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.clearToast()
    }
  }

  val hazeState = remember { HazeState() }

  MediatekaTheme(colors = activeTheme) {
   CompositionLocalProvider(LocalHapticsEnabled provides uiState.settings.vibrationEnabled) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Всё, что должно просвечивать сквозь стеклянные панели (фон, снег, экраны), лежит внутри hazeSource.
      // Раньше источником был только Scaffold, а фон лежал рядом, поэтому под стеклом нечего было размывать.
      Box(
        modifier = Modifier
          .fillMaxSize()
          .hazeSource(state = hazeState)
      ) {
      // Ambient background mesh canvas
      AmbientBackground(
        bgType = uiState.settings.bgType,
        currentTrack = uiState.currentTrack,
        bgColorByCover = uiState.settings.bgColorByCover,
        bgMoveOnlyWhilePlaying = uiState.settings.bgMoveOnlyWhilePlaying,
        isPlaying = uiState.isPlaying,
        bgSpeed = uiState.settings.bgSpeed,
        bgIntensity = uiState.settings.bgIntensity
      )

      // Snowflakes overlay when enabled in Settings
      SnowOverlay(
        enabled = uiState.settings.snowEnabled,
        amount = uiState.settings.snowAmount
      )

      Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          // Main content views
          if (uiState.activeArtist != null) {
            ArtistScreen(
              artist = uiState.activeArtist!!,
              tracks = uiState.tracks,
              currentTrack = uiState.currentTrack,
              favIconStyle = uiState.settings.favIcon,
              onBackClick = { viewModel.closeArtist() },
              onTrackClick = { viewModel.playTrack(it) },
              onTrackLongClick = { viewModel.openContextMenu(it) },
              onPlayArtist = {
                uiState.tracks.firstOrNull { it.artist.contains(uiState.activeArtist!!.name, true) }
                  ?.let { viewModel.playTrack(it) }
              },
              onShuffleArtist = {
                viewModel.toggleShuffle()
                uiState.tracks.filter { it.artist.contains(uiState.activeArtist!!.name, true) }
                  .shuffled()
                  .firstOrNull()
                  ?.let { viewModel.playTrack(it) }
              },
              onFavoriteToggle = { viewModel.toggleFavorite(it) },
              onDownloadClick = { viewModel.downloadTrack(it) },
              onSelectRelatedArtist = { viewModel.openArtist(it) },
              enabledAnimations = animationsEnabled
            )
          } else {
            when (uiState.currentTab) {
              "home" -> HomeScreen(
                tracks = uiState.tracks,
                recommendations = uiState.recommendations,
                isFeedLoading = uiState.isFeedLoading,
                currentTrack = uiState.currentTrack,
                isPlaying = uiState.isPlaying || uiState.isBuffering,
                favIconStyle = uiState.settings.favIcon,
                onTrackClick = { viewModel.playTrack(it) },
                onTrackLongClick = { viewModel.openContextMenu(it) },
                onPlayClick = { viewModel.togglePlayPause() },
                onDiceClick = { viewModel.playRandom() },
                onMixClick = { viewModel.playMix() },
                onRefreshRecs = { viewModel.refreshFeed(manual = true) },
                onTuneRecs = {
                  viewModel.setSettingsTab("feed")
                  viewModel.setTab("settings")
                },
                onAddFolderClick = {},
                onAddFilesClick = {},
                onAudioFilesPicked = { uris -> viewModel.addPickedAudioFiles(uris) },
                onFavoritesTileClick = {
                  viewModel.setLibraryFilter("fav")
                  viewModel.setTab("lib")
                },
                onDownloadsTileClick = {
                  viewModel.setLibraryFilter("dl")
                  viewModel.setTab("lib")
                },
                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                onDownloadClick = { viewModel.downloadTrack(it) },
                enabledAnimations = animationsEnabled
              )

              "search" -> SearchScreen(
                query = uiState.searchQuery,
                recentQueries = uiState.recentQueries,
                tracks = uiState.tracks,
                onlineSearchResults = uiState.onlineSearchResults,
                isSearchingOnline = uiState.isSearchingOnline,
                currentTrack = uiState.currentTrack,
                favIconStyle = uiState.settings.favIcon,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onClearRecentQueries = { viewModel.clearRecentQueries() },
                onTrackClick = { viewModel.playTrack(it) },
                onTrackLongClick = { viewModel.openContextMenu(it) },
                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                onDownloadClick = { viewModel.downloadTrack(it) },
                enabledAnimations = animationsEnabled
              )

              "lib" -> LibraryScreen(
                tracks = uiState.tracks,
                currentTrack = uiState.currentTrack,
                currentFilter = uiState.libraryFilter,
                favIconStyle = uiState.settings.favIcon,
                onFilterChange = { viewModel.setLibraryFilter(it) },
                onTrackClick = { viewModel.playTrack(it) },
                onTrackLongClick = { viewModel.openContextMenu(it) },
                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                onDownloadClick = { viewModel.downloadTrack(it) },
                onDeleteClick = { viewModel.deleteTrack(it) },
                onAddMusicClick = {},
                onAudioFilesPicked = { uris -> viewModel.addPickedAudioFiles(uris) },
                onArtistClick = { viewModel.openArtist(it) },
                enabledAnimations = animationsEnabled
              )

              "settings" -> SettingsScreen(
                settings = uiState.settings,
                activeTab = uiState.settingsTab,
                onTabChange = { viewModel.setSettingsTab(it) },
                onThemeSelect = { viewModel.setTheme(it) },
                onGlassSelect = { viewModel.setGlassStyle(it) },
                onSnowToggle = { viewModel.toggleSnow() },
                onSnowAmountSelect = { viewModel.setSnowAmount(it) },
                onFavIconSelect = { viewModel.setFavIcon(it) },
                onVibrationToggle = { viewModel.toggleVibration() },
                onAnimationsToggle = { viewModel.toggleAnimations() },
                onBgTypeSelect = { viewModel.setBgType(it) },
                onBgColorByCoverToggle = { viewModel.toggleBgColorByCover() },
                onBgMoveOnlyPlayingToggle = { viewModel.toggleBgMoveOnlyPlaying() },
                onBgSpeedSelect = { viewModel.setBgSpeed(it) },
                onBgIntensitySelect = { viewModel.setBgIntensity(it) },
                onFeedLanguageSelect = { viewModel.setFeedLanguage(it) },
                onAddMyArtist = { viewModel.addMyArtist(it) },
                onRemoveMyArtist = { viewModel.removeMyArtist(it) },
                onTracksPerArtistSelect = { viewModel.setTracksPerArtist(it) },
                onFeedSizeSelect = { viewModel.setFeedSize(it) },
                onFeedAutoPlayToggle = { viewModel.toggleFeedAutoPlay() },
                onFeedAutoScrollToggle = { viewModel.toggleFeedAutoScroll() },
                onDownloadDestSelect = { viewModel.setDownloadDestination(it) },
                onSourceTasteToggle = { viewModel.toggleSourceTaste() },
                onSourceAudiusToggle = { viewModel.toggleSourceAudius() },
                onSourceYtToggle = { viewModel.toggleSourceYt() },
                onRefreshFeed = { viewModel.refreshFeed(manual = true) },
                onResetTaste = { viewModel.resetTaste() },
                ymAccount = uiState.ymAccount,
                ymError = uiState.ymError,
                ymLogin = uiState.ymLogin,
                onYmLogin = { viewModel.startYmLogin() },
                onYmCancelLogin = { viewModel.cancelYmLogin() },
                onYmSignOut = { viewModel.signOutYm() },
                onYmToken = { viewModel.setYmToken(it) },
                onYmGateway = { viewModel.setYmGateway(it) },
                onCobaltUrl = { viewModel.setCobaltUrl(it) },
                onCobaltKey = { viewModel.setCobaltKey(it) },
                onYtWorker = { viewModel.setYtWorker(it) },
                onPipedUrl = { viewModel.setPipedUrl(it) },
                onYtApiKey = { viewModel.setYtApiKey(it) },
                onShowToast = { viewModel.showToast(it) },
                enabledAnimations = animationsEnabled
              )
            }
          }
        }
      }

      } // конец hazeSource

      // Нижняя панель: мини-плеер над навбаром, обе стеклянные
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val currentTrack = uiState.currentTrack
        if (currentTrack != null && !uiState.isPlayerSheetOpen) {
          val progress = if (currentTrack.durationSeconds > 0) {
            uiState.currentPositionSeconds.toFloat() / currentTrack.durationSeconds
          } else 0f

          MiniPlayer(
            track = currentTrack,
            isPlaying = uiState.isPlaying || uiState.isBuffering,
            progress = progress,
            glassStyle = uiState.settings.glassStyle,
            hazeState = hazeState,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onNextClick = { viewModel.nextTrack() },
            onPlayerClick = { viewModel.openPlayerSheet() },
            enabledAnimations = animationsEnabled
          )
        }

        BottomNavBar(
          currentTab = uiState.currentTab,
          onTabSelected = { viewModel.setTab(it) },
          glassStyle = uiState.settings.glassStyle,
          coverHue = uiState.currentTrack?.coverHue?.toFloat() ?: 260f,
          hazeState = hazeState,
          enabledAnimations = animationsEnabled
        )
      }

      // Overlay 3: Fullscreen Player Sheet
      PlayerBottomSheet(
        visible = uiState.isPlayerSheetOpen,
        track = uiState.currentTrack,
        isPlaying = uiState.isPlaying || uiState.isBuffering,
        currentPositionSeconds = uiState.currentPositionSeconds,
        isShuffle = uiState.isShuffle,
        repeatMode = uiState.repeatMode,
        coverShapeIndex = uiState.coverShapeIndex,
        favIconStyle = uiState.settings.favIcon,
        volume = uiState.currentVolume,
        onVolumeChange = { viewModel.setVolume(it) },
        onClose = { viewModel.closePlayerSheet() },
        onPlayPause = { viewModel.togglePlayPause() },
        onNext = { viewModel.nextTrack() },
        onPrev = { viewModel.previousTrack() },
        onShuffle = { viewModel.toggleShuffle() },
        onRepeat = { viewModel.cycleRepeat() },
        onCoverShapeCycle = { viewModel.cycleCoverShape() },
        onFavoriteToggle = {
          uiState.currentTrack?.let { viewModel.toggleFavorite(it.id) }
        },
        onDownload = {
          uiState.currentTrack?.let { viewModel.downloadTrack(it) }
        },
        onArtistClick = { artistName ->
          viewModel.closePlayerSheet()
          viewModel.openArtist(artistName)
        },
        onSeek = { seconds -> viewModel.seekTo(seconds) },
        enabledAnimations = animationsEnabled
      )

      // Overlay 4: Context Menu
      TrackContextMenu(
        track = uiState.contextMenuTrack,
        onDismiss = { viewModel.closeContextMenu() },
        onPlayNext = {
          uiState.contextMenuTrack?.let { viewModel.playNext(it) }
        },
        onAddToQueue = {
          uiState.contextMenuTrack?.let { viewModel.addToQueue(it) }
        },
        onToggleFavorite = {
          uiState.contextMenuTrack?.let { viewModel.toggleFavorite(it.id) }
        },
        onDownload = {
          uiState.contextMenuTrack?.let { viewModel.downloadTrack(it) }
        },
        onGoToArtist = { artistName ->
          viewModel.openArtist(artistName)
        },
        onDelete = {
          uiState.contextMenuTrack?.let { viewModel.deleteTrack(it) }
        }
      )
    }
   }
  }
}
