package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalMediatekaColors = staticCompositionLocalOf { ThemeViolet }

val MaterialTheme.mediatekaColors: MediatekaColors
  @Composable
  @ReadOnlyComposable
  get() = LocalMediatekaColors.current

@Composable
fun MediatekaTheme(
  colors: MediatekaColors = ThemeViolet,
  content: @Composable () -> Unit
) {
  val m3Colors: ColorScheme = if (colors.isDark) {
    darkColorScheme(
      primary = colors.p,
      onPrimary = colors.onp,
      primaryContainer = colors.pc,
      onPrimaryContainer = colors.onpc,
      secondary = colors.te,
      surface = colors.s1,
      onSurface = colors.tx,
      background = colors.bg,
      onBackground = colors.tx,
      outline = colors.ln
    )
  } else {
    lightColorScheme(
      primary = colors.p,
      onPrimary = colors.onp,
      primaryContainer = colors.pc,
      onPrimaryContainer = colors.onpc,
      secondary = colors.te,
      surface = colors.s1,
      onSurface = colors.tx,
      background = colors.bg,
      onBackground = colors.tx,
      outline = colors.ln
    )
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colors.bg.toArgb()
        window.navigationBarColor = colors.bg.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
          isAppearanceLightStatusBars = !colors.isDark
          isAppearanceLightNavigationBars = !colors.isDark
        }
      }
    }
  }

  CompositionLocalProvider(LocalMediatekaColors provides colors) {
    MaterialTheme(
      colorScheme = m3Colors,
      typography = AppTypography,
      content = content
    )
  }
}
