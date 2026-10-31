package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.UnboundedFontFamily
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale

@Composable
fun BentoCards(
  favoritesCount: Int,
  downloadsCount: Int,
  favIconStyle: String,
  onShuffleClick: () -> Unit,
  onFavoritesClick: () -> Unit,
  onDownloadsClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(182.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Big Left Card: "Вперемешку"
    Box(
      modifier = Modifier
        .weight(1.25f)
        .fillMaxSize()
        .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 14.dp, bottomStart = 40.dp, bottomEnd = 14.dp))
        .background(colors.p)
        .pressScale(
          pressedScale = 0.96f,
          durationMillis = 300,
          easing = SpringEasing,
          enabled = enabledAnimations,
          onClick = onShuffleClick
        )
    ) {
      // Decorative background circle blob
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 34.dp, y = 34.dp)
          .size(150.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.22f))
      )

      // Content
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = colors.onp,
          modifier = Modifier.size(32.dp)
        )

        Column {
          Text(
            text = "Вперемешку",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = colors.onp,
            lineHeight = 22.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Случайные треки из медиатеки",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = colors.onp.copy(alpha = 0.8f),
            lineHeight = 15.sp
          )
        }
      }
    }

    // Right Column: Two stacked cards
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top Right: "Избранное"
      val favBg = Color(
        red = (colors.te.red * 0.3f + colors.s1.red * 0.7f),
        green = (colors.te.green * 0.3f + colors.s1.green * 0.7f),
        blue = (colors.te.blue * 0.3f + colors.s1.blue * 0.7f)
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 40.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
          .background(favBg)
          .pressScale(
            pressedScale = 0.96f,
            durationMillis = 300,
            easing = SpringEasing,
            enabled = enabledAnimations,
            onClick = onFavoritesClick
          )
      ) {
        Icon(
          imageVector = if (favIconStyle == "heart") Icons.Default.Favorite else Icons.Default.Check,
          contentDescription = null,
          tint = colors.tx.copy(alpha = 0.9f),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 14.dp)
            .size(24.dp)
        )

        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp)
        ) {
          Text(
            text = "Избранное",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            ),
            color = colors.tx
          )
          Text(
            text = "$favoritesCount трека",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = colors.mu
          )
        }
      }

      // Bottom Right: "Загрузки"
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 40.dp, bottomEnd = 14.dp))
          .background(colors.pc)
          .pressScale(
            pressedScale = 0.96f,
            durationMillis = 300,
            easing = SpringEasing,
            enabled = enabledAnimations,
            onClick = onDownloadsClick
          )
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = null,
          tint = colors.onpc.copy(alpha = 0.9f),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 14.dp)
            .size(24.dp)
        )

        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(14.dp)
        ) {
          Text(
            text = "Загрузки",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            ),
            color = colors.onpc
          )
          Text(
            text = "$downloadsCount треков",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = colors.onpc.copy(alpha = 0.75f)
          )
        }
      }
    }
  }
}
