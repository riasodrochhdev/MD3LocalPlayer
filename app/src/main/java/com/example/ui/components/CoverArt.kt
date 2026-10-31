package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.mediatekaColors

@Composable
fun CoverArt(
  coverHue: Int,
  coverUrl: String? = null,
  modifier: Modifier = Modifier,
  shapeType: Int = 0, // 0: rounded rect, 1: circle, 2: expressive squircle
  elevation: Dp = 0.dp
) {
  val colors = MaterialTheme.mediatekaColors
  val shape: Shape = when (shapeType) {
    1 -> CircleShape
    2 -> RoundedCornerShape(38) // Expressive squircle shape
    else -> RoundedCornerShape(16.dp)
  }

  // Calculate HSL gradient colors based on hue
  val h = coverHue.toFloat()
  val startColor = Color.hsl(hue = h, saturation = 0.42f, lightness = 0.42f)
  val endColor = Color.hsl(hue = (h + 40f) % 360f, saturation = 0.45f, lightness = 0.22f)

  val shadowModifier = if (elevation > 0.dp) {
    Modifier.shadow(elevation, shape)
  } else {
    Modifier
  }

  Box(
    modifier = modifier
      .then(shadowModifier)
      .clip(shape)
      .background(Brush.linearGradient(listOf(startColor, endColor))),
    contentAlignment = Alignment.Center
  ) {
    if (!coverUrl.isNullOrEmpty()) {
      AsyncImage(
        model = coverUrl,
        contentDescription = "Cover",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        error = rememberVectorPainter(Icons.Default.MusicNote)
      )
    } else {
      Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = Color.White.copy(alpha = 0.75f)
      )
    }
  }
}
