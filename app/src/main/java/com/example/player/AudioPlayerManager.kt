package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.example.model.Track
import com.example.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
  /** Для какого трека сейчас заданы длительность и позиция. */
  val trackId: Long = -1L,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val volume: Float = 1.0f
)

class AudioPlayerManager(private val context: Context) {
  private val player = PlayerHolder.get(context)
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var positionJob: Job? = null

  private val _playbackState = MutableStateFlow(PlaybackState(volume = player.volume))
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  /** Трек доиграл до конца. */
  var onEnded: (() -> Unit)? = null

  /** Не удалось воспроизвести текущую ссылку. */
  var onError: ((PlaybackException) -> Unit)? = null

  val hasMedia: Boolean
    get() = player.mediaItemCount > 0

  private val listener = object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
      _playbackState.update { it.copy(isPlaying = isPlaying) }
      if (isPlaying) startPositionTracking() else stopPositionTracking()
    }

    override fun onPlaybackStateChanged(state: Int) {
      _playbackState.update { it.copy(isBuffering = state == Player.STATE_BUFFERING) }
      when (state) {
        Player.STATE_READY -> {
          val dur = player.duration
          _playbackState.update { it.copy(durationMs = if (dur > 0) dur else it.durationMs) }
        }
        Player.STATE_ENDED -> onEnded?.invoke()
        else -> Unit
      }
    }

    override fun onPlayerError(error: PlaybackException) {
      _playbackState.update { it.copy(isPlaying = false, isBuffering = false) }
      onError?.invoke(error)
    }
  }

  init {
    player.addListener(listener)
    // Сервис уведомлений поднимает фоновое воспроизведение и медиа-уведомление
    try {
      context.applicationContext.startService(Intent(context.applicationContext, PlaybackService::class.java))
    } catch (_: Exception) {
    }
  }

  fun play(track: Track, url: String) {
    _playbackState.update {
      it.copy(trackId = track.id, currentPositionMs = 0L, durationMs = track.durationSeconds * 1000L, isBuffering = true)
    }
    val item = MediaItem.Builder()
      .setUri(Uri.parse(url))
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(track.title)
          .setArtist(track.artist)
          .setAlbumTitle(track.album)
          .setArtworkUri(track.coverUrl?.takeIf { it.startsWith("http") }?.let { Uri.parse(it) })
          .build()
      )
      .build()
    player.setMediaItem(item)
    player.prepare()
    player.play()
  }

  fun togglePlayPause() {
    if (player.isPlaying) player.pause() else player.play()
  }

  fun pause() {
    player.pause()
  }

  fun seekToMs(positionMs: Long) {
    player.seekTo(positionMs.coerceAtLeast(0L))
    _playbackState.update { it.copy(currentPositionMs = positionMs) }
  }

  fun restart() {
    player.seekTo(0L)
    player.play()
  }

  fun setVolume(volume: Float) {
    val v = volume.coerceIn(0f, 1f)
    player.volume = v
    _playbackState.update { it.copy(volume = v) }
  }

  private fun startPositionTracking() {
    positionJob?.cancel()
    positionJob = scope.launch {
      while (isActive) {
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration
        _playbackState.update {
          it.copy(currentPositionMs = pos, durationMs = if (dur > 0) dur else it.durationMs)
        }
        delay(500L)
      }
    }
  }

  private fun stopPositionTracking() {
    positionJob?.cancel()
    positionJob = null
  }

  /** Отписывается от плеера, но сам плеер не трогает: он живёт, пока работает музыка в фоне. */
  fun release() {
    stopPositionTracking()
    player.removeListener(listener)
    onEnded = null
    onError = null
    scope.cancel()
  }
}
