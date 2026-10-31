package com.example.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

/**
 * Один ExoPlayer на всё приложение. Раньше интерфейс и сервис уведомлений создавали каждый свой плеер,
 * поэтому уведомления и фоновая работа не были связаны с тем, что играет в приложении.
 */
object PlayerHolder {
  @Volatile
  private var player: ExoPlayer? = null

  fun get(context: Context): ExoPlayer {
    return player ?: synchronized(this) {
      player ?: build(context.applicationContext).also { player = it }
    }
  }

  private fun build(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .setUsage(C.USAGE_MEDIA)
          .build(),
        true
      )
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_NETWORK)
      .build()
  }
}
