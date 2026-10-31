package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale
import kotlin.math.sin

@Composable
fun PlayerBottomSheet(
  visible: Boolean,
  track: Track?,
  isPlaying: Boolean,
  currentPositionSeconds: Int,
  isShuffle: Boolean,
  repeatMode: Int,
  coverShapeIndex: Int,
  favIconStyle: String,
  volume: Float = 1.0f,
  onVolumeChange: (Float) -> Unit = {},
  onClose: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrev: () -> Unit,
  onShuffle: () -> Unit,
  onRepeat: () -> Unit,
  onCoverShapeCycle: () -> Unit,
  onFavoriteToggle: () -> Unit,
  onDownload: () -> Unit,
  onArtistClick: (String) -> Unit,
  onSeek: (Int) -> Unit,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors

  AnimatedVisibility(
    visible = visible && track != null,
    enter = slideInVertically(
      initialOffsetY = { it },
      animationSpec = tween(durationMillis = if (enabledAnimations) 500 else 0, easing = EaseEasing)
    ) + fadeIn(tween(durationMillis = if (enabledAnimations) 500 else 0, easing = EaseEasing)),
    exit = slideOutVertically(
      targetOffsetY = { it },
      animationSpec = tween(durationMillis = if (enabledAnimations) 500 else 0, easing = EaseEasing)
    ) + fadeOut(tween(durationMillis = if (enabledAnimations) 500 else 0, easing = EaseEasing))
  ) {
    if (track == null) return@AnimatedVisibility

    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, coverShapeIndex, enabledAnimations) {
      if (isPlaying && coverShapeIndex != 0 && enabledAnimations) {
        while (true) {
          rotationAnim.animateTo(
            targetValue = rotationAnim.value + 360f,
            animationSpec = tween(durationMillis = 24000, easing = LinearEasing)
          )
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(colors.bg.copy(alpha = 0.95f))
        // поглощаем нажатия, иначе они «проваливались» на навбар и мини-плеер под шторкой
        .pointerInput(Unit) { detectTapGestures { } }
    ) {
      DriftingAmbientSpots(isPlaying = isPlaying, enabledAnimations = enabledAnimations)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp)
          .padding(top = 44.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top action bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          PressBox(
            onClick = onClose,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(colors.s1)
              .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onClose)
          ) {
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = "Collapse",
              tint = colors.tx,
              modifier = Modifier.size(28.dp)
            )
          }

          PressBox(
            onClick = onCoverShapeCycle,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(colors.s1)
              .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onCoverShapeCycle)
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Cover shape",
              tint = colors.p,
              modifier = Modifier.size(24.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Album Art with rotation if active
        Box(
          modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f)
            .graphicsLayer {
              if (coverShapeIndex != 0) {
                rotationZ = rotationAnim.value % 360f
              }
            },
          contentAlignment = Alignment.Center
        ) {
          CoverArt(
            coverHue = track.coverHue,
            coverUrl = track.coverUrl,
            shapeType = coverShapeIndex,
            elevation = 28.dp,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title + Artist + Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = track.title,
              style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = colors.tx,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = track.artist,
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                color = colors.mu
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onArtistClick(track.artist) }
              )
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = onDownload,
              modifier = Modifier.size(44.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = if (track.isDownloaded) colors.p else colors.mu,
                modifier = Modifier.size(24.dp)
              )
            }

            IconButton(
              onClick = onFavoriteToggle,
              modifier = Modifier.size(44.dp)
            ) {
              val favIcon = if (favIconStyle == "heart") {
                if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
              } else {
                if (track.isFavorite) Icons.Default.Check else Icons.Default.FavoriteBorder
              }
              Icon(
                imageVector = favIcon,
                contentDescription = "Favorite",
                tint = if (track.isFavorite) colors.p else colors.mu,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wave Progress Track
        Column(modifier = Modifier.fillMaxWidth()) {
          val progressRatio = if (track.durationSeconds > 0) {
            (currentPositionSeconds.toFloat() / track.durationSeconds).coerceIn(0f, 1f)
          } else 0f

          WaveProgressBar(
            progress = progressRatio,
            isPlaying = isPlaying,
            onSeek = { ratio ->
              onSeek((ratio * track.durationSeconds).toInt())
            },
            activeColor = colors.p,
            trackColor = colors.tx.copy(alpha = 0.22f),
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp),
            enabledAnimations = enabledAnimations
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "%d:%02d".format(currentPositionSeconds / 60, currentPositionSeconds % 60),
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = colors.mu
            )
            Text(
              text = track.formattedDuration,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = colors.mu
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback controls row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onShuffle,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shuffle,
              contentDescription = "Shuffle",
              tint = if (isShuffle) colors.p else colors.mu,
              modifier = Modifier.size(24.dp)
            )
          }

          IconButton(
            onClick = onPrev,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = "Previous",
              tint = colors.tx,
              modifier = Modifier.size(30.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(colors.p)
              .pressScale(
                pressedScale = 0.9f,
                durationMillis = 280,
                easing = SpringEasing,
                enabled = enabledAnimations,
                onClick = onPlayPause
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = colors.onp,
              modifier = Modifier.size(38.dp)
            )
          }

          IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next",
              tint = colors.tx,
              modifier = Modifier.size(30.dp)
            )
          }

          IconButton(
            onClick = onRepeat,
            modifier = Modifier.size(48.dp)
          ) {
            val repeatIcon = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat
            Icon(
              imageVector = repeatIcon,
              contentDescription = "Repeat",
              tint = if (repeatMode > 0) colors.p else colors.mu,
              modifier = Modifier.size(24.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Working Volume Control
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Volume",
            tint = colors.mu,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
              thumbColor = colors.p,
              activeTrackColor = colors.p,
              inactiveTrackColor = colors.tx.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun DriftingAmbientSpots(isPlaying: Boolean, enabledAnimations: Boolean) {
  val colors = MaterialTheme.mediatekaColors
  val phaseAnim = remember { Animatable(0f) }

  LaunchedEffect(isPlaying, enabledAnimations) {
    if (isPlaying && enabledAnimations) {
      while (true) {
        phaseAnim.animateTo(
          targetValue = phaseAnim.value + (2f * Math.PI.toFloat()),
          animationSpec = tween(durationMillis = 20000, easing = LinearEasing)
        )
      }
    }
  }

  Canvas(modifier = Modifier.fillMaxSize()) {
    val phase = phaseAnim.value
    val offsetX1 = sin(phase) * size.width * 0.04f
    val offsetY1 = sin(phase * 0.7f) * size.height * 0.03f

    val offsetX2 = sin(phase * 1.2f + 1f) * size.width * 0.04f
    val offsetY2 = sin(phase * 0.9f + 1f) * size.height * 0.03f

    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(colors.p.copy(alpha = 0.35f), Color.Transparent),
        center = Offset(size.width * 0.28f + offsetX1, size.height * 0.30f + offsetY1),
        radius = size.width * 0.55f
      ),
      center = Offset(size.width * 0.28f + offsetX1, size.height * 0.30f + offsetY1),
      radius = size.width * 0.55f
    )

    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(colors.te.copy(alpha = 0.30f), Color.Transparent),
        center = Offset(size.width * 0.76f + offsetX2, size.height * 0.74f + offsetY2),
        radius = size.width * 0.55f
      ),
      center = Offset(size.width * 0.76f + offsetX2, size.height * 0.74f + offsetY2),
      radius = size.width * 0.55f
    )
  }
}

@Composable
fun WaveProgressBar(
  progress: Float,
  isPlaying: Boolean,
  onSeek: (Float) -> Unit,
  activeColor: Color,
  trackColor: Color,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  var isDragging by remember { mutableStateOf(false) }

  val wavePhaseAnim = remember { Animatable(0f) }
  LaunchedEffect(isPlaying, enabledAnimations) {
    if (isPlaying && enabledAnimations) {
      while (true) {
        wavePhaseAnim.animateTo(
          targetValue = wavePhaseAnim.value + 40f,
          animationSpec = tween(durationMillis = 900, easing = LinearEasing)
        )
      }
    }
  }

  val waveAmplitudeRatio by animateFloatAsState(
    targetValue = if (isPlaying) 1f else 0f,
    animationSpec = tween(
      durationMillis = if (enabledAnimations) 500 else 0,
      easing = EaseEasing
    ),
    label = "waveAmplitude"
  )

  val thumbHeightVal by animateFloatAsState(
    targetValue = if (isDragging) 38f else 30f,
    animationSpec = tween(
      durationMillis = if (enabledAnimations) 280 else 0,
      easing = SpringEasing
    ),
    label = "thumbHeight"
  )

  Canvas(
    modifier = modifier
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          val ratio = (offset.x / size.width).coerceIn(0f, 1f)
          onSeek(ratio)
        }
      }
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { isDragging = true },
          onDragEnd = { isDragging = false },
          onDragCancel = { isDragging = false }
        ) { change, _ ->
          change.consume()
          val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
          onSeek(ratio)
        }
      }
  ) {
    val width = size.width
    val height = size.height
    val centerY = height / 2f
    val activeX = width * progress

    if (activeX < width) {
      drawLine(
        color = trackColor,
        start = Offset(activeX + 4.dp.toPx(), centerY),
        end = Offset(width, centerY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
      )
      drawCircle(
        color = activeColor,
        radius = 3.dp.toPx(),
        center = Offset(width - 2.dp.toPx(), centerY)
      )
    }

    if (activeX > 0) {
      val amplitude = 5.dp.toPx() * waveAmplitudeRatio
      if (amplitude > 0.1f) {
        val path = Path()
        path.moveTo(0f, centerY)
        val wavelength = 40.dp.toPx()
        val phasePx = wavePhaseAnim.value
        var x = 0f
        while (x <= activeX) {
          val y = centerY + sin((x + phasePx) / wavelength * 2f * Math.PI.toFloat()) * amplitude
          path.lineTo(x, y)
          x += 2f
        }
        drawPath(
          path = path,
          color = activeColor,
          style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
      } else {
        drawLine(
          color = activeColor,
          start = Offset(0f, centerY),
          end = Offset(activeX, centerY),
          strokeWidth = 4.dp.toPx(),
          cap = StrokeCap.Round
        )
      }

      val barHalfHeight = (thumbHeightVal.dp.toPx()) / 2f
      drawLine(
        color = activeColor,
        start = Offset(activeX, centerY - barHalfHeight),
        end = Offset(activeX, centerY + barHalfHeight),
        strokeWidth = if (isDragging) 3.dp.toPx() else 4.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }
}
