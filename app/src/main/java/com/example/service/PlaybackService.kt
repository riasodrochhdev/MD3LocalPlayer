package com.example.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.player.PlayerHolder

/** Медиа-уведомление и управление с экрана блокировки/наушников. Использует тот же плеер, что и приложение. */
class PlaybackService : MediaSessionService() {
  private var mediaSession: MediaSession? = null

  override fun onCreate() {
    super.onCreate()
    val session = MediaSession.Builder(this, PlayerHolder.get(this)).build()
    mediaSession = session
    addSession(session)
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onDestroy() {
    mediaSession?.release()
    mediaSession = null
    super.onDestroy()
  }
}
