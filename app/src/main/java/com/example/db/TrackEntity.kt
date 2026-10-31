package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Source
import com.example.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
  @PrimaryKey val id: Long,
  val title: String,
  val artist: String,
  val album: String,
  val durationSeconds: Int,
  val coverHue: Int,
  val coverUrl: String?,
  val isFavorite: Boolean,
  val isDownloaded: Boolean,
  val uriString: String? = null,
  val addedTimestamp: Long = System.currentTimeMillis(),
  val source: String = Source.LOCAL,
  val sourceId: String? = null
) {
  fun toTrack() = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationSeconds = durationSeconds,
    coverHue = coverHue,
    coverUrl = coverUrl,
    streamUrl = uriString,
    isFavorite = isFavorite,
    isDownloaded = isDownloaded,
    source = source,
    sourceId = sourceId
  )
}

fun Track.toEntity(uriString: String? = null) = TrackEntity(
  id = id,
  title = title,
  artist = artist,
  album = album,
  durationSeconds = durationSeconds,
  coverHue = coverHue,
  coverUrl = coverUrl,
  isFavorite = isFavorite,
  isDownloaded = isDownloaded,
  uriString = uriString ?: this.streamUrl,
  source = source,
  sourceId = sourceId
)
