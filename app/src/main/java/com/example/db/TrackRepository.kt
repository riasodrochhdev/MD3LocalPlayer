package com.example.db

import kotlinx.coroutines.flow.Flow

class TrackRepository(private val trackDao: TrackDao) {
  val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
  val favoriteTracks: Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()
  val downloadedTracks: Flow<List<TrackEntity>> = trackDao.getDownloadedTracks()

  suspend fun initDefaultTracksIfEmpty(defaultTracks: List<TrackEntity>) {
    if (trackDao.getTracksCount() == 0) {
      trackDao.insertOrUpdateTracks(defaultTracks)
    }
  }

  suspend fun insertOrUpdate(track: TrackEntity) {
    trackDao.insertOrUpdateTrack(track)
  }

  suspend fun insertOrUpdateAll(tracks: List<TrackEntity>) {
    trackDao.insertOrUpdateTracks(tracks)
  }

  suspend fun insertIfAbsent(tracks: List<TrackEntity>) {
    trackDao.insertIfAbsent(tracks)
  }

  suspend fun getById(id: Long): TrackEntity? = trackDao.getById(id)

  suspend fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
    trackDao.updateFavorite(trackId, isFavorite)
  }

  suspend fun setDownloaded(trackId: Long, isDownloaded: Boolean) {
    trackDao.updateDownload(trackId, isDownloaded)
  }

  suspend fun setDownloadedFile(trackId: Long, uri: String?) {
    trackDao.updateDownloadWithUri(trackId, uri != null, uri)
  }

  suspend fun deleteTrack(trackId: Long) {
    trackDao.deleteTrack(trackId)
  }
}
