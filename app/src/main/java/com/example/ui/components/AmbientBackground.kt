package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.model.Track
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.mediatekaColors
import kotlin.math.sin

@Composable
fun AmbientBackground(
  bgType: String = "fluid", // "off", "fluid", "waves"
  currentTrack: Track? = null,
  bgColorByCover: Boolean = true,
  bgMoveOnlyWhilePlaying: Boolean = false,
  isPlaying: Boolean = false,
  bgSpeed: Int = 2, // 1: slow, 2: medium, 3: fast
  bgIntensity: Int = 2, // 1: soft, 2: medium, 3: vivid
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.mediatekaColors

  if (bgType == "off") {
    Box(modifier = modifier.fillMaxSize().background(colors.bg))
    return
  }

  // Calculate target colors based on cover hue or theme colors
  val (targetPrimary, targetSecondary, targetTertiary) = if (bgColorByCover && currentTrack != null) {
    val h = currentTrack.coverHue.toFloat()
    Triple(
      Color.hsl(hue = h, saturation = 0.55f, lightness = 0.42f),
      Color.hsl(hue = (h + 40f) % 360f, saturation = 0.50f, lightness = 0.36f),
      Color.hsl(hue = (h - 45f + 360f) % 360f, saturation = 0.50f, lightness = 0.32f)
    )
  } else {
    Triple(colors.a1, colors.a2, colors.a3)
  }

  val animPrimary by animateColorAsState(targetValue = targetPrimary, animationSpec = tween(1200, easing = EaseEasing), label = "bgPrimary")
  val animSecondary by animateColorAsState(targetValue = targetSecondary, animationSpec = tween(1200, easing = EaseEasing), label = "bgSecondary")
  val animTertiary by animateColorAsState(targetValue = targetTertiary, animationSpec = tween(1200, easing = EaseEasing), label = "bgTertiary")

  val intensityFactor = when (bgIntensity) {
    1 -> 0.45f
    3 -> 0.90f
    else -> 0.65f
  }

  val speedDuration = when (bgSpeed) {
    1 -> 24000
    3 -> 8000
    else -> 14000
  }

  val phaseAnim = remember { Animatable(0f) }
  val shouldAnimate = !bgMoveOnlyWhilePlaying || isPlaying

  LaunchedEffect(shouldAnimate, speedDuration) {
    if (shouldAnimate) {
      while (true) {
        phaseAnim.animateTo(
          targetValue = phaseAnim.value + (2f * Math.PI.toFloat()),
          animationSpec = tween(durationMillis = speedDuration, easing = LinearEasing)
        )
      }
    }
  }

  Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val phase = phaseAnim.value

      if (bgType == "waves") {
        // Draw 3 flowing animated gradient waves
        val waveHeight = h * 0.04f * intensityFactor

        for (k in 0..2) {
          val waveColor = when (k) {
            0 -> animPrimary.copy(alpha = 0.45f * intensityFactor)
            1 -> animSecondary.copy(alpha = 0.35f * intensityFactor)
            else -> animTertiary.copy(alpha = 0.25f * intensityFactor)
          }

          val baseY = h * (0.55f + 0.12f * k)
          val path = Path()
          path.moveTo(0f, h)

          var x = 0f
          while (x <= w) {
            val u = x / w
            val y = baseY + sin(u * 6.28f * (1.2f + 0.3f * k) + phase * (0.8f + 0.3f * k) + k * 1.5f) * waveHeight
            path.lineTo(x, y)
            x += 8f
          }
          path.lineTo(w, h)
          path.close()

          drawPath(
            path = path,
            brush = Brush.verticalGradient(
              colors = listOf(waveColor, Color.Transparent),
              startY = h * 0.4f,
              endY = h
            )
          )
        }
      } else {
        // "fluid" / spots: 3 drifting radial ambient spots
        val offsetX1 = sin(phase) * w * 0.08f
        val offsetY1 = sin(phase * 0.7f) * h * 0.06f

        val offsetX2 = sin(phase * 1.2f + 1f) * w * 0.08f
        val offsetY2 = sin(phase * 0.9f + 1f) * h * 0.06f

        val offsetX3 = sin(phase * 0.8f + 2f) * w * 0.06f
        val offsetY3 = sin(phase * 1.1f + 2f) * h * 0.05f

        // Spot 1: Top-Right
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(animSecondary.copy(alpha = 0.65f * intensityFactor), Color.Transparent),
            center = Offset(w * 0.95f + offsetX1, -h * 0.04f + offsetY1),
            radius = w * 0.85f
          ),
          center = Offset(w * 0.95f + offsetX1, -h * 0.04f + offsetY1),
          radius = w * 0.85f
        )

        // Spot 2: Bottom-Left
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(animPrimary.copy(alpha = 0.70f * intensityFactor), Color.Transparent),
            center = Offset(-w * 0.05f + offsetX2, h * 0.78f + offsetY2),
            radius = w * 0.95f
          ),
          center = Offset(-w * 0.05f + offsetX2, h * 0.78f + offsetY2),
          radius = w * 0.95f
        )

        // Spot 3: Bottom-Right
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(animTertiary.copy(alpha = 0.45f * intensityFactor), Color.Transparent),
            center = Offset(w * 0.88f + offsetX3, h * 1.02f + offsetY3),
            radius = w * 0.65f
          ),
          center = Offset(w * 0.88f + offsetX3, h * 1.02f + offsetY3),
          radius = w * 0.65f
        )
      }
    }
  }
}
