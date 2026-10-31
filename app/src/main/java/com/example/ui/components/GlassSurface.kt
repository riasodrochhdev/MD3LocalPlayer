package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.mediatekaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Стеклянная панель для навбара и мини-плеера.
 *
 *  off    - плотная панель без прозрачности;
 *  blur   - настоящее размытие того, что лежит под панелью (фон, обложки, списки), с лёгким оттенком от обложки;
 *  liquid - то же размытие плюс «жидкое стекло»: диагональный блик, световая кромка по контуру
 *           и подсветка под пальцем, как в прототипе (index.html).
 *
 * Размытие работает, только если фон и контент находятся внутри hazeSource (см. MainActivity).
 */
@Composable
fun GlassSurface(
  style: String,
  hazeState: HazeState?,
  tint: Color,
  cornerRadius: Dp,
  solidColor: Color,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val colors = MaterialTheme.mediatekaColors
  val shape = RoundedCornerShape(cornerRadius)

  // подсветка под пальцем (только в liquid)
  var touch by remember { mutableStateOf<Offset?>(null) }
  val lastTouch = remember { mutableStateOf(Offset.Zero) }
  val glowAlpha by animateFloatAsState(
    targetValue = if (touch != null) 1f else 0f,
    animationSpec = tween(durationMillis = if (touch != null) 120 else 450),
    label = "glassGlow"
  )

  val touchTracker = if (style == "liquid") {
    Modifier.pointerInput(Unit) {
      awaitPointerEventScope {
        while (true) {
          // смотрим на касания, но ничего не потребляем: кнопки внутри работают как обычно
          val event = awaitPointerEvent(PointerEventPass.Initial)
          val change = event.changes.firstOrNull()
          if (change != null && change.pressed) {
            lastTouch.value = change.position
            touch = change.position
          } else {
            touch = null
          }
        }
      }
    }
  } else {
    Modifier
  }

  Box(modifier = modifier.then(touchTracker)) {
    val layer: Modifier = when (style) {
      "off" -> Modifier
        .shadow(14.dp, shape, spotColor = Color(0x80000000))
        .clip(shape)
        .background(solidColor)
        .border(1.dp, colors.ln, shape)

      "liquid" -> {
        val glass = lerp(colors.bg, tint, 0.55f)
        Modifier
          .shadow(26.dp, shape, ambientColor = Color(0x33000000), spotColor = tint.copy(alpha = 0.65f))
          .clip(shape)
          .then(
            if (hazeState != null) {
              Modifier.hazeEffect(
                state = hazeState,
                style = HazeStyle(
                  backgroundColor = colors.bg,
                  blurRadius = 22.dp,
                  tint = HazeTint(glass.copy(alpha = 0.26f)),
                  noiseFactor = 0.04f
                )
              )
            } else {
              Modifier.background(glass.copy(alpha = 0.85f))
            }
          )
          // диагональный блик стекла: 135deg, светлее по краям, прозрачнее в середине
          .background(
            Brush.linearGradient(
              0f to Color(0x3DFFFFFF),
              0.42f to Color(0x08FFFFFF),
              1f to Color(0x24FFFFFF)
            )
          )
          .border(
            width = 1.dp,
            brush = Brush.linearGradient(
              0f to Color(0x70FFFFFF),
              0.5f to Color(0x14FFFFFF),
              1f to Color(0x45FFFFFF)
            ),
            shape = shape
          )
          .drawWithContent {
            drawContent()
            val sw = 1.6.dp.toPx()
            val inset = sw / 2f
            val r = (cornerRadius.toPx() - inset).coerceAtLeast(0f)
            val rectSize = Size(size.width - sw, size.height - sw)
            // верхняя кромка: яркий блик, затухающий вниз
            drawRoundRect(
              brush = Brush.verticalGradient(
                0f to Color(0x99FFFFFF),
                0.45f to Color(0x00FFFFFF),
                startY = 0f,
                endY = size.height
              ),
              topLeft = Offset(inset, inset),
              size = rectSize,
              cornerRadius = CornerRadius(r, r),
              style = Stroke(width = sw)
            )
            // нижняя кромка: слабый отсвет
            drawRoundRect(
              brush = Brush.verticalGradient(
                0.6f to Color(0x00FFFFFF),
                1f to Color(0x30FFFFFF),
                startY = 0f,
                endY = size.height
              ),
              topLeft = Offset(inset, inset),
              size = rectSize,
              cornerRadius = CornerRadius(r, r),
              style = Stroke(width = sw)
            )
            // свечение под пальцем
            if (glowAlpha > 0.01f) {
              val c = lastTouch.value
              val radius = 96.dp.toPx()
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(Color.White.copy(alpha = 0.34f * glowAlpha), Color.Transparent),
                  center = c,
                  radius = radius
                ),
                radius = radius,
                center = c
              )
            }
          }
      }

      else -> {
        val glass = lerp(colors.bg, tint, 0.45f)
        Modifier
          .shadow(22.dp, shape, ambientColor = Color(0x30000000), spotColor = tint.copy(alpha = 0.55f))
          .clip(shape)
          .then(
            if (hazeState != null) {
              Modifier.hazeEffect(
                state = hazeState,
                style = HazeStyle(
                  backgroundColor = colors.bg,
                  blurRadius = 26.dp,
                  tint = HazeTint(glass.copy(alpha = 0.42f)),
                  noiseFactor = 0.03f
                )
              )
            } else {
              Modifier.background(glass.copy(alpha = 0.9f))
            }
          )
          .background(
            Brush.verticalGradient(
              listOf(Color(0x1AFFFFFF), Color(0x00FFFFFF))
            )
          )
          .border(
            width = 1.dp,
            brush = Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x12FFFFFF))),
            shape = shape
          )
      }
    }

    Box(modifier = Modifier.matchParentSize().then(layer))
    content()
  }
}
