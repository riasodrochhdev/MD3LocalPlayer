package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.mediatekaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextMenu(
  track: Track?,
  onDismiss: () -> Unit,
  onPlayNext: () -> Unit,
  onAddToQueue: () -> Unit,
  onToggleFavorite: () -> Unit,
  onDownload: () -> Unit,
  onGoToArtist: (String) -> Unit,
  onDelete: () -> Unit
) {
  if (track == null) return
  val colors = MaterialTheme.mediatekaColors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.s1,
    dragHandle = null,
    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
      // Header: Cover + Title + Artist
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.size(52.dp)) {
          CoverArt(
            coverHue = track.coverHue,
            coverUrl = track.coverUrl,
            shapeType = 0,
            modifier = Modifier.size(52.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp
            ),
            color = colors.tx,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = colors.mu,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Divider(color = colors.ln, thickness = 1.dp)
      Spacer(modifier = Modifier.height(8.dp))

      // Menu actions
      ContextMenuItem(
        icon = Icons.Default.PlayArrow,
        label = "Играть следующим",
        onClick = {
          onPlayNext()
          onDismiss()
        }
      )

      ContextMenuItem(
        icon = Icons.Default.QueueMusic,
        label = "Добавить в очередь",
        onClick = {
          onAddToQueue()
          onDismiss()
        }
      )

      ContextMenuItem(
        icon = if (track.isFavorite) Icons.Default.Check else Icons.Default.Add,
        label = if (track.isFavorite) "Убрать из избранного" else "В избранное",
        onClick = {
          onToggleFavorite()
          onDismiss()
        }
      )

      ContextMenuItem(
        icon = Icons.Default.Download,
        label = if (track.isDownloaded) "Уже скачано" else "Скачать",
        onClick = {
          onDownload()
          onDismiss()
        }
      )

      ContextMenuItem(
        icon = Icons.Default.Person,
        label = "К исполнителю",
        onClick = {
          onGoToArtist(track.artist)
          onDismiss()
        }
      )

      ContextMenuItem(
        icon = Icons.Default.Delete,
        label = "Удалить из медиатеки",
        onClick = {
          onDelete()
          onDismiss()
        }
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ContextMenuItem(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.mediatekaColors

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp)
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = colors.p,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
      ),
      color = colors.tx
    )
  }
}
