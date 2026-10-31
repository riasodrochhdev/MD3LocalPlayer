package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

data class Snowflake(
  var x: Float,
  var y: Float,
  var radius: Float,
  var isBig: Boolean,
  var vy: Float,
  var sway: Float,
  var freq: Float,
  var phase: Float,
  var angle: Float,
  var rotSpeed: Float,
  var alpha: Float
)

@Composable
fun SnowOverlay(
  enabled: Boolean,
  amount: Int, // 1 = few (28), 2 = medium (55), 3 = heavy (100)
  modifier: Modifier = Modifier
) {
  if (!enabled) return

  val particleCount = remember(amount) {
    when (amount) {
      1 -> 28
      3 -> 100
      else -> 55
    }
  }

  var frameTimeNanos by remember { mutableLongStateOf(0L) }
  val lastFrame = remember { LongArray(1) }

  // Каждый кадр обновляем состояние; Canvas ниже его читает и поэтому перерисовывается.
  // Раньше состояние нигде не читалось, и снег стоял на месте.
  LaunchedEffect(enabled, amount) {
    while (true) {
      withFrameNanos { frameTimeNanos = it }
    }
  }

  val snowflakes = remember(particleCount) {
    List(particleCount) { createSnowflake(initial = true) }
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val now = frameTimeNanos
    val dt = if (lastFrame[0] == 0L) 0.016f else ((now - lastFrame[0]) / 1_000_000_000f).coerceIn(0f, 0.05f)
    lastFrame[0] = now

    snowflakes.forEach { flake ->
      // Update flake position
      flake.y += flake.vy * dt
      flake.phase += flake.freq * dt
      flake.angle += flake.rotSpeed * dt

      if (flake.y - flake.radius > canvasHeight + 20f) {
        resetSnowflake(flake, canvasWidth)
      }

      val px = flake.x + sin(flake.phase) * flake.sway

      if (flake.isBig) {
        // Draw 6-arm branching snowflake geometry
        withTransform({
          translate(left = px, top = flake.y)
          rotate(degrees = Math.toDegrees(flake.angle.toDouble()).toFloat(), pivot = Offset.Zero)
        }) {
          val armLength = flake.radius * 2.2f
          val strokeW = (flake.radius / 3.5f).coerceAtLeast(1.2f)
          val color = Color.White.copy(alpha = flake.alpha)

          for (i in 0 until 6) {
            val rad = i * (Math.PI / 3.0)
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()

            val endX = cosA * armLength
            val endY = sinA * armLength

            drawLine(
              color = color,
              start = Offset.Zero,
              end = Offset(endX, endY),
              strokeWidth = strokeW,
              cap = StrokeCap.Round
            )

            // Branch lines on arms
            val branchX = cosA * armLength * 0.6f
            val branchY = sinA * armLength * 0.6f
            val branchLen = armLength * 0.35f

            for (side in listOf(-1, 1)) {
              val branchRad = rad + side * 0.8
              val bEndX = branchX + cos(branchRad).toFloat() * branchLen
              val bEndY = branchY + sin(branchRad).toFloat() * branchLen

              drawLine(
                color = color,
                start = Offset(branchX, branchY),
                end = Offset(bEndX, bEndY),
                strokeWidth = strokeW * 0.8f,
                cap = StrokeCap.Round
              )
            }
          }
        }
      } else {
        // Draw standard circular snowflake dot
        drawCircle(
          color = Color.White.copy(alpha = flake.alpha),
          radius = flake.radius,
          center = Offset(px, flake.y)
        )
      }
    }
  }
}

private fun createSnowflake(initial: Boolean): Snowflake {
  val isBig = Math.random() < 0.28
  val radius = if (isBig) (4.5f + Math.random().toFloat() * 4f) else (1f + Math.random().toFloat() * 2.3f)
  val vy = 35f + radius * 12f + Math.random().toFloat() * 25f
  val sway = 12f + Math.random().toFloat() * 24f
  val freq = 0.35f + Math.random().toFloat() * 0.7f
  val phase = (Math.random() * Math.PI * 2).toFloat()
  val angle = (Math.random() * Math.PI * 2).toFloat()
  val rotSpeed = ((Math.random() - 0.5) * 1.2).toFloat()
  val alpha = if (isBig) (0.55f + Math.random().toFloat() * 0.35f) else (0.40f + Math.random().toFloat() * 0.45f)

  val x = (Math.random() * 1080).toFloat()
  val y = if (initial) (Math.random() * 2000).toFloat() else -20f

  return Snowflake(x, y, radius, isBig, vy, sway, freq, phase, angle, rotSpeed, alpha)
}

private fun resetSnowflake(flake: Snowflake, canvasWidth: Float) {
  flake.x = (Math.random() * canvasWidth.coerceAtLeast(100f)).toFloat()
  flake.y = -20f
  flake.phase = (Math.random() * Math.PI * 2).toFloat()
  flake.angle = (Math.random() * Math.PI * 2).toFloat()
}
