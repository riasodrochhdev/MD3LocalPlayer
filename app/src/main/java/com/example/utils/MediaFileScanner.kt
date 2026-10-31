package com.example.utils

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.model.Ids
import com.example.model.Source
import com.example.model.Track

object MediaFileScanner {
  private val AUDIO_EXT = Regex("\\.(mp3|m4a|flac|wav|ogg|opus|aac)$", RegexOption.IGNORE_CASE)

  /** Все музыкальные файлы на телефоне. id стабильный (от id в MediaStore), поэтому избранное не теряется при повторном скане. */
  fun scanDeviceAudioFiles(context: Context): List<Pair<Track, Uri>> {
    val resultList = mutableListOf<Pair<Track, Uri>>()
    val resolver: ContentResolver = context.contentResolver

    val projection = arrayOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST,
      MediaStore.Audio.Media.ALBUM,
      MediaStore.Audio.Media.DURATION
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
      resolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
          val mediaId = cursor.getLong(idColumn)
          val title = cursor.getString(titleColumn) ?: "Без названия"
          val artist = cursor.getString(artistColumn) ?: "Неизвестный исполнитель"
          val album = cursor.getString(albumColumn) ?: "Медиатека"
          val durationMs = cursor.getLong(durationColumn)

          val contentUri = Uri.withAppendedPath(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaId.toString()
          )

          val track = Track(
            id = 10_000L + mediaId,
            title = title,
            artist = if (artist == "<unknown>") "Неизвестный исполнитель" else artist,
            album = album,
            durationSeconds = (durationMs / 1000L).toInt().coerceAtLeast(1),
            coverHue = Ids.hue(title),
            coverUrl = null,
            streamUrl = contentUri.toString(),
            isFavorite = false,
            isDownloaded = true,
            source = Source.LOCAL
          )

          resultList.add(Pair(track, contentUri))
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return resultList
  }

  /** Один файл, выбранный через проводник: название, исполнитель и длительность берём из тегов. */
  fun parseUriToTrack(context: Context, uri: Uri): Pair<Track, Uri> {
    var title: String? = null
    var artist: String? = null
    var durationMs = 0L
    var displayName = ""

    try {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (idx != -1) displayName = cursor.getString(idx).orEmpty()
        }
      }
    } catch (_: Exception) {
    }

    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
      artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
      durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {
      }
    }

    // если в тегах пусто, берём из имени файла: «Исполнитель - Название.mp3»
    val cleanName = displayName.replace(AUDIO_EXT, "")
    if (title.isNullOrBlank()) {
      if (cleanName.contains(" - ")) {
        val parts = cleanName.split(" - ", limit = 2)
        if (artist.isNullOrBlank()) artist = parts[0].trim()
        title = parts[1].trim()
      } else {
        title = cleanName.ifBlank { "Новый трек" }
      }
    }

    val finalTitle = title ?: "Новый трек"
    val track = Track(
      id = Ids.of(Source.LOCAL, uri.toString()),
      title = finalTitle,
      artist = artist?.takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель",
      album = "Загрузки",
      durationSeconds = if (durationMs > 0) (durationMs / 1000L).toInt() else 180,
      coverHue = Ids.hue(finalTitle),
      coverUrl = null,
      streamUrl = uri.toString(),
      isFavorite = false,
      isDownloaded = true,
      source = Source.LOCAL
    )
    return Pair(track, uri)
  }

  /** Все аудиофайлы выбранной папки, включая вложенные. */
  fun scanTree(context: Context, treeUri: Uri, limit: Int = 3000): List<Pair<Track, Uri>> {
    val out = ArrayList<Pair<Track, Uri>>()
    val queue = ArrayDeque<String>()
    queue.add(DocumentsContract.getTreeDocumentId(treeUri))

    while (queue.isNotEmpty() && out.size < limit) {
      val parentId = queue.removeFirst()
      val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
      try {
        context.contentResolver.query(
          children,
          arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
          ),
          null,
          null,
          null
        )?.use { c ->
          while (c.moveToNext() && out.size < limit) {
            val docId = c.getString(0) ?: continue
            val name = c.getString(1).orEmpty()
            val mime = c.getString(2).orEmpty()
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
              queue.add(docId)
            } else if (mime.startsWith("audio/") || AUDIO_EXT.containsMatchIn(name)) {
              val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
              out.add(parseUriToTrack(context, docUri))
            }
          }
        }
      } catch (_: Exception) {
      }
    }
    return out
  }
}
