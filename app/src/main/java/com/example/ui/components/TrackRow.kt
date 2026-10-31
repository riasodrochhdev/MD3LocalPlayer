package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
  track: Track,
  isCurrent: Boolean,
  favIconStyle: String,
  showDeleteButton: Boolean = false,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onFavoriteToggle: () -> Unit,
  onDownloadClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  val backgroundColor = if (isCurrent) colors.pc.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Transparent

  Row(
    modifier = modifier
      .testTag("track_row_${track.id}")
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(backgroundColor)
      .pressScale(
        pressedScale = 0.985f,
        durationMillis = 250,
        easing = EaseEasing,
        enabled = enabledAnimations,
        onLongClick = onLongClick,
        onClick = onClick
      )
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Cover
    Box(modifier = Modifier.size(48.dp)) {
      CoverArt(
        coverHue = track.coverHue,
        coverUrl = track.coverUrl,
        shapeType = 0,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    // Title and Artist
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = track.title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 15.sp
        ),
        color = if (isCurrent) colors.p else colors.tx,
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

    // Download button
    PressBox(
      onClick = onDownloadClick,
      modifier = Modifier
        .size(38.dp)
        .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onDownloadClick)
    ) {
      Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Download",
        tint = if (track.isDownloaded) colors.p else colors.mu.copy(alpha = 0.8f),
        modifier = Modifier.size(20.dp)
      )
    }

    // Favorite button (plus or check, or heart in circle)
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(if (track.isFavorite) colors.p.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent)
        .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onFavoriteToggle),
      contentAlignment = Alignment.Center
    ) {
      val favIcon = if (favIconStyle == "heart") {
        if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
      } else {
        if (track.isFavorite) Icons.Default.Check else Icons.Default.Add
      }
      Icon(
        imageVector = favIcon,
        contentDescription = "Favorite",
        tint = if (track.isFavorite) colors.p else colors.mu,
        modifier = Modifier.size(18.dp)
      )
    }

    // Optional Delete button (for Library view as in photo 4)
    if (showDeleteButton && onDeleteClick != null) {
      PressBox(
        onClick = onDeleteClick,
        modifier = Modifier
          .size(36.dp)
          .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onDeleteClick)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Delete",
          tint = colors.mu,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Duration
    Text(
      text = track.formattedDuration,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
      color = colors.mu
    )
  }
}
