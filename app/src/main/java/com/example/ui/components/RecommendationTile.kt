package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale

@Composable
fun RecommendationTile(
  track: Track,
  isCurrent: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors

  Column(
    modifier = modifier
      .testTag("rec_tile_${track.id}")
      .width(156.dp)
      .clip(RoundedCornerShape(28.dp))
      .pressScale(
        pressedScale = 0.9f,
        durationMillis = 280,
        easing = SpringEasing,
        enabled = enabledAnimations,
        onLongClick = onLongClick,
        onClick = onClick
      )
  ) {
    // 1:1 Big square cover with 28dp radius
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
    ) {
      CoverArt(
        coverHue = track.coverHue,
        coverUrl = track.coverUrl,
        shapeType = 0,
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(28.dp))
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = track.title,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
      ),
      color = if (isCurrent) colors.p else colors.tx,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = track.artist,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
      color = colors.mu,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
