package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale
import dev.chrisbanes.haze.HazeState

@Composable
fun MiniPlayer(
  track: Track,
  isPlaying: Boolean,
  progress: Float, // 0.0f .. 1.0f
  glassStyle: String = "blur", // "off", "blur", "liquid"
  hazeState: HazeState? = null,
  onPlayPauseClick: () -> Unit,
  onNextClick: () -> Unit,
  onPlayerClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  val shape = RoundedCornerShape(34.dp)

  // 500ms ease entrance animation on appearance
  val entranceProgress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    if (enabledAnimations) {
      entranceProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, easing = EaseEasing)
      )
    } else {
      entranceProgress.snapTo(1f)
    }
  }

  val targetTrackTint = remember(track.coverHue) {
    val hueFloat = track.coverHue.toFloat()
    Color.hsl(hue = (hueFloat % 360f + 360f) % 360f, saturation = 0.55f, lightness = 0.28f)
  }
  val animatedTrackTint by animateColorAsState(
    targetValue = targetTrackTint,
    animationSpec = tween(durationMillis = 600, easing = EaseEasing),
    label = "miniTrackTint"
  )

  Box(
    modifier = modifier
      .padding(horizontal = 12.dp)
      .graphicsLayer {
        val p = entranceProgress.value
        alpha = p
        scaleX = 0.99f + (0.01f * p)
        scaleY = 0.99f + (0.01f * p)
        translationY = (1f - p) * 14.dp.toPx()
      }
  ) {
    GlassSurface(
      style = glassStyle,
      hazeState = hazeState,
      tint = animatedTrackTint,
      cornerRadius = 34.dp,
      solidColor = colors.pc,
      modifier = Modifier
        .fillMaxWidth()
        .pressScale(
          pressedScale = 0.985f,
          durationMillis = 250,
          easing = EaseEasing,
          enabled = enabledAnimations,
          onClick = onPlayerClick
        )
    ) {
      // полоска прогресса по нижнему краю, обрезанная по скруглению панели
      Box(modifier = Modifier.matchParentSize().clip(shape)) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
            .height(3.dp)
            .background(colors.p)
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(68.dp)
          .padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .border(2.5.dp, colors.p.copy(alpha = 0.6f), CircleShape)
        ) {
          CoverArt(
            coverHue = track.coverHue,
            coverUrl = track.coverUrl,
            shapeType = 1,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp
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

        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.p)
            .pressScale(
              pressedScale = 0.88f,
              durationMillis = 280,
              easing = SpringEasing,
              enabled = enabledAnimations,
              onClick = onPlayPauseClick
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = colors.onp,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressScale(
              pressedScale = 0.9f,
              durationMillis = 250,
              easing = SpringEasing,
              enabled = enabledAnimations,
              onClick = onNextClick
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = colors.tx,
            modifier = Modifier.size(26.dp)
          )
        }
      }
    }
  }
}
