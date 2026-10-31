package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

// Exact curves from CSS:
// var(--ease): cubic-bezier(.2, .8, .2, 1)
val EaseEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)

// var(--sp): cubic-bezier(.34, 1.56, .64, 1)
val SpringEasing: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

/** Включена ли вибрация-отклик (настройка «Вибрация»). */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Нажатие с «пружинным» сжатием.
 *
 * Раньше здесь был самописный pointerInput, который терял клики внутри прокручиваемых списков
 * (из-за этого «не нажималась половина настроек»), а долгое нажатие вообще не поддерживалось.
 * Теперь это обычный combinedClickable: он правильно работает со скроллом, доступностью и долгим нажатием.
 *
 * enabled: включена ли анимация сжатия (сам клик работает всегда).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressScale(
  pressedScale: Float = 0.9f,
  durationMillis: Int = 280,
  easing: Easing = SpringEasing,
  enabled: Boolean = true,
  onLongClick: (() -> Unit)? = null,
  onClick: (() -> Unit)? = null
): Modifier {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val haptic = LocalHapticFeedback.current
  val hapticsOn = LocalHapticsEnabled.current

  val scale by animateFloatAsState(
    targetValue = if (enabled && pressed) pressedScale else 1f,
    animationSpec = tween(durationMillis = durationMillis, easing = easing),
    label = "pressScale"
  )

  val scaled = this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }

  if (onClick == null && onLongClick == null) return scaled

  val longHandler: (() -> Unit)? = onLongClick?.let { lc ->
    {
      if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      lc()
    }
  }

  return scaled.combinedClickable(
    interactionSource = interaction,
    indication = null,
    role = Role.Button,
    onLongClick = longHandler,
    onClick = {
      if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick?.invoke()
    }
  )
}
