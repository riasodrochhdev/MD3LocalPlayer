package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Замена IconButton для кнопок, у которых клик и анимацию нажатия уже задаёт Modifier.pressScale.
 * У настоящего IconButton свой внутренний клик перехватывал нажатие, и анимация сжатия не срабатывала.
 * Параметр onClick оставлен для совместимости вызовов: клик обрабатывает pressScale в modifier.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun PressBox(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
    content = content
  )
}
