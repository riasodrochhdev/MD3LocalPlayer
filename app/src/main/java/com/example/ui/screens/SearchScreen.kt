package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.components.TrackRow
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.UnboundedFontFamily
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
  query: String,
  recentQueries: List<String>,
  tracks: List<Track>,
  onlineSearchResults: List<Track> = emptyList(),
  isSearchingOnline: Boolean = false,
  currentTrack: Track?,
  favIconStyle: String,
  onQueryChange: (String) -> Unit,
  onClearRecentQueries: () -> Unit,
  onTrackClick: (Track) -> Unit,
  onTrackLongClick: (Track) -> Unit,
  onFavoriteToggle: (Long) -> Unit,
  onDownloadClick: (Track) -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  val focusManager = LocalFocusManager.current

  val localResults = remember(query, tracks) {
    if (query.isBlank()) emptyList()
    else tracks.filter {
      it.title.contains(query, ignoreCase = true) ||
          it.artist.contains(query, ignoreCase = true) ||
          it.album.contains(query, ignoreCase = true)
    }
  }

  // Screen entrance animation (450ms ease, slide 14dp, scale 0.99->1, alpha 0->1)
  val screenEntrance = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    if (enabledAnimations) {
      screenEntrance.animateTo(1f, tween(450, easing = EaseEasing))
    } else {
      screenEntrance.snapTo(1f)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer {
        val p = screenEntrance.value
        alpha = p
        scaleX = 0.99f + (0.01f * p)
        scaleY = 0.99f + (0.01f * p)
        translationY = (1f - p) * 14.dp.toPx()
      }
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = 20.dp,
        end = 20.dp,
        top = 20.dp,
        bottom = 210.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Title
      item {
        Text(
          text = "Поиск",
          fontFamily = UnboundedFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 32.sp,
          color = colors.tx
        )
      }

      // Search bar with dense frosted glass border
      item {
        OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          placeholder = {
            Text(
              text = "Песни, исполнители, YouTube, Audius...",
              style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
              color = colors.mu.copy(alpha = 0.7f),
              maxLines = 1
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = colors.p,
              modifier = Modifier.size(24.dp)
            )
          },
          trailingIcon = {
            if (isSearchingOnline) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.p,
                strokeWidth = 2.dp
              )
            } else if (query.isNotEmpty()) {
              IconButton(onClick = { onQueryChange("") }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear",
                  tint = colors.mu
                )
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.s1.copy(alpha = 0.92f),
            unfocusedContainerColor = colors.s1.copy(alpha = 0.92f),
            focusedBorderColor = colors.p,
            unfocusedBorderColor = colors.p.copy(alpha = 0.6f),
            focusedTextColor = colors.tx,
            unfocusedTextColor = colors.tx
          ),
          shape = RoundedCornerShape(32.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
        )
      }

      // Recent queries chips
      if (query.isEmpty() && recentQueries.isNotEmpty()) {
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Недавние запросы",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 15.sp
                ),
                color = colors.mu
              )

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(colors.s1)
                  .pressScale(pressedScale = 0.95f, enabled = enabledAnimations, onClick = onClearRecentQueries)
                  .padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                Text(
                  text = "Очистить всё",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                  ),
                  color = colors.p
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              recentQueries.forEach { chipQuery ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.s1)
                    .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                      onQueryChange(chipQuery)
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                  Text(
                    text = chipQuery,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Medium,
                      fontSize = 14.sp
                    ),
                    color = colors.tx
                  )
                }
              }
            }
          }
        }
      }

      // Local Search Results
      if (query.isNotEmpty() && localResults.isNotEmpty()) {
        item {
          Text(
            text = "Локальные файлы",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.tx,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
          )
        }

        itemsIndexed(localResults, key = { _, track -> "local_${track.id}" }) { index, track ->
          val staggerProgress = remember { Animatable(0f) }
          LaunchedEffect(Unit) {
            if (enabledAnimations) {
              delay((index * 40L).coerceAtMost(320L))
              staggerProgress.animateTo(1f, tween(550, easing = SpringEasing))
            } else {
              staggerProgress.snapTo(1f)
            }
          }

          TrackRow(
            track = track,
            isCurrent = currentTrack?.id == track.id,
            favIconStyle = favIconStyle,
            onClick = { onTrackClick(track) },
            onLongClick = { onTrackLongClick(track) },
            onFavoriteToggle = { onFavoriteToggle(track.id) },
            onDownloadClick = { onDownloadClick(track) },
            enabledAnimations = enabledAnimations,
            modifier = Modifier.graphicsLayer {
              val p = staggerProgress.value
              alpha = p
              scaleX = 0.97f + (0.03f * p)
              scaleY = 0.97f + (0.03f * p)
              translationY = (1f - p) * 16.dp.toPx()
            }
          )
        }
      }

      // Online Search Results (Audius & YouTube Music)
      if (query.isNotEmpty() && onlineSearchResults.isNotEmpty()) {
        item {
          Text(
            text = "Онлайн треки (YouTube Music & Audius)",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.p,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
          )
        }

        itemsIndexed(onlineSearchResults, key = { _, track -> "online_${track.id}" }) { index, track ->
          val staggerProgress = remember { Animatable(0f) }
          LaunchedEffect(Unit) {
            if (enabledAnimations) {
              delay((index * 40L).coerceAtMost(320L))
              staggerProgress.animateTo(1f, tween(550, easing = SpringEasing))
            } else {
              staggerProgress.snapTo(1f)
            }
          }

          TrackRow(
            track = track,
            isCurrent = currentTrack?.id == track.id,
            favIconStyle = favIconStyle,
            onClick = { onTrackClick(track) },
            onLongClick = { onTrackLongClick(track) },
            onFavoriteToggle = { onFavoriteToggle(track.id) },
            onDownloadClick = { onDownloadClick(track) },
            enabledAnimations = enabledAnimations,
            modifier = Modifier.graphicsLayer {
              val p = staggerProgress.value
              alpha = p
              scaleX = 0.97f + (0.03f * p)
              scaleY = 0.97f + (0.03f * p)
              translationY = (1f - p) * 16.dp.toPx()
            }
          )
        }
      }

      // Online Services Status Cards
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Подключённые сервисы",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.tx
          )

          listOf(
            "YouTube Music" to "Поиск и стриминг аудиозаписей",
            "Audius" to "Открытый каталог и независимые релизы",
            "Яндекс Музыка" to "Моя волна и региональный каталог"
          ).forEach { (serviceName, serviceDesc) ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.s1.copy(alpha = 0.92f))
                .border(1.dp, colors.ln, RoundedCornerShape(20.dp))
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(colors.p.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.MusicNote,
                  contentDescription = null,
                  tint = colors.p,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = serviceName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = colors.tx
                )
                Text(
                  text = serviceDesc,
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.mu
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(colors.p.copy(alpha = 0.2f))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Активен",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = colors.p
                )
              }
            }
          }
        }
      }

      // Empty state
      if (query.isNotEmpty() && localResults.isEmpty() && onlineSearchResults.isEmpty() && !isSearchingOnline) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = colors.p,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Ничего не найдено",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = colors.tx
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Проверьте запрос или попробуйте другой жанр.",
              style = MaterialTheme.typography.bodyMedium,
              color = colors.mu,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      // Footer
      item {
        Text(
          text = "#вайбкод | riasodrochh made",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
          color = colors.mu.copy(alpha = 0.7f),
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 12.dp)
        )
      }
    }
  }
}
