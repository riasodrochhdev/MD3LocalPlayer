package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
  @Query("SELECT * FROM tracks ORDER BY addedTimestamp DESC")
  fun getAllTracks(): Flow<List<TrackEntity>>

  @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY addedTimestamp DESC")
  fun getFavoriteTracks(): Flow<List<TrackEntity>>

  @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY addedTimestamp DESC")
  fun getDownloadedTracks(): Flow<List<TrackEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateTracks(tracks: List<TrackEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateTrack(track: TrackEntity)

  /** Вставка без перезаписи: при повторном сканировании избранное и загрузки не теряются. */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIfAbsent(tracks: List<TrackEntity>)

  @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
  suspend fun getById(trackId: Long): TrackEntity?

  @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
  suspend fun updateFavorite(trackId: Long, isFavorite: Boolean)

  @Query("UPDATE tracks SET isDownloaded = :isDownloaded WHERE id = :trackId")
  suspend fun updateDownload(trackId: Long, isDownloaded: Boolean)

  @Query("UPDATE tracks SET isDownloaded = :isDownloaded, uriString = :uri WHERE id = :trackId")
  suspend fun updateDownloadWithUri(trackId: Long, isDownloaded: Boolean, uri: String?)

  @Query("DELETE FROM tracks WHERE id = :trackId")
  suspend fun deleteTrack(trackId: Long)

  @Query("SELECT COUNT(*) FROM tracks")
  suspend fun getTracksCount(): Int
}
