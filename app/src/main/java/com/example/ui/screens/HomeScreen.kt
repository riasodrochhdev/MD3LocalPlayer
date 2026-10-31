package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.components.BentoCards
import com.example.ui.components.RecommendationTile
import com.example.ui.components.TrackRow
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.UnboundedFontFamily
import com.example.ui.theme.mediatekaColors
import com.example.ui.components.PressBox
import com.example.ui.theme.pressScale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
  tracks: List<Track>,
  recommendations: List<Track>,
  isFeedLoading: Boolean = false,
  currentTrack: Track?,
  isPlaying: Boolean,
  favIconStyle: String,
  onTrackClick: (Track) -> Unit,
  onTrackLongClick: (Track) -> Unit,
  onPlayClick: () -> Unit,
  onDiceClick: () -> Unit,
  onMixClick: () -> Unit,
  onRefreshRecs: () -> Unit,
  onTuneRecs: () -> Unit,
  onAddFolderClick: () -> Unit,
  onAddFilesClick: () -> Unit,
  onAudioFilesPicked: (List<android.net.Uri>) -> Unit = {},
  onFavoritesTileClick: () -> Unit,
  onDownloadsTileClick: () -> Unit,
  onFavoriteToggle: (Long) -> Unit,
  onDownloadClick: (Track) -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  val favorites = remember(tracks) { tracks.filter { it.isFavorite } }
  val downloads = remember(tracks) { tracks.filter { it.isDownloaded } }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      onAudioFilesPicked(uris)
    }
  }

  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      onAudioFilesPicked(listOf(uri))
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
        top = 16.dp,
        bottom = 210.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(colors.p, colors.te))),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = "Logo",
              tint = colors.onp,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Text(
            text = "MD3 Player",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = colors.tx,
            modifier = Modifier.weight(1f)
          )

          PressBox(
            onClick = { folderPickerLauncher.launch(null) },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(colors.s1)
              .border(1.dp, colors.ln, CircleShape)
              .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = { folderPickerLauncher.launch(null) })
          ) {
            Icon(
              imageVector = Icons.Default.Folder,
              contentDescription = "Add Folder",
              tint = colors.tx,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          PressBox(
            onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(colors.s1)
              .border(1.dp, colors.ln, CircleShape)
              .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = { filePickerLauncher.launch(arrayOf("audio/*")) })
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add Files",
              tint = colors.tx,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }

      // Hero Section
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Text(
            text = "Ваша\nподборка",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.035).sp,
            color = colors.tx
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentTrack?.let { "${it.artist} - ${it.title}" } ?: "Ржавый бит - Вайбкод",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = colors.mu,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              Spacer(modifier = Modifier.height(14.dp))

              // "Мой микс" button
              Row(
                modifier = Modifier
                  .height(44.dp)
                  .clip(RoundedCornerShape(22.dp))
                  .background(colors.p)
                  .pressScale(
                    pressedScale = 0.9f,
                    durationMillis = 280,
                    easing = SpringEasing,
                    enabled = enabledAnimations,
                    onClick = onMixClick
                  )
                  .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = colors.onp,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Мой микс",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                  ),
                  color = colors.onp
                )
              }
            }

            // Big Play Button + Dice
            Box(
              modifier = Modifier
                .padding(start = 12.dp)
                .size(104.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(96.dp)
                  .clip(CircleShape)
                  .background(colors.pc)
                  .pressScale(
                    pressedScale = 0.9f,
                    durationMillis = 280,
                    easing = SpringEasing,
                    enabled = enabledAnimations,
                    onClick = onPlayClick
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Play",
                  tint = colors.onpc,
                  modifier = Modifier.size(42.dp)
                )
              }

              // Small Dice button: rotates 25° and scales to 1.08 on press (300 ms spring)
              val diceScaleAnim = remember { Animatable(1f) }
              val diceRotateAnim = remember { Animatable(0f) }

              Box(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .offset(x = 4.dp, y = 4.dp)
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(colors.p)
                  .border(4.dp, colors.bg, CircleShape)
                  .graphicsLayer {
                    scaleX = diceScaleAnim.value
                    scaleY = diceScaleAnim.value
                    rotationZ = diceRotateAnim.value
                  }
                  .clickable {
                    onDiceClick()
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Casino,
                  contentDescription = "Random Track",
                  tint = colors.onp,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }
        }
      }

      // Bento Cards
      item {
        BentoCards(
          favoritesCount = favorites.size,
          downloadsCount = downloads.size,
          favIconStyle = favIconStyle,
          onShuffleClick = onDiceClick,
          onFavoritesClick = onFavoritesTileClick,
          onDownloadsClick = onDownloadsTileClick,
          modifier = Modifier.padding(top = 10.dp),
          enabledAnimations = enabledAnimations
        )
      }

      // Recommendations
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Рекомендации",
              fontFamily = UnboundedFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 19.sp,
              color = colors.tx
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(colors.s1)
                  .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onTuneRecs),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Tune,
                  contentDescription = "Configure feed",
                  tint = colors.p,
                  modifier = Modifier.size(20.dp)
                )
              }

              Box(
                modifier = Modifier
                  .height(36.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(colors.s1)
                  .pressScale(pressedScale = 0.95f, enabled = enabledAnimations, onClick = onRefreshRecs)
                  .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Обновить",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                  ),
                  color = colors.p
                )
              }
            }
          }

          if (recommendations.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.s1.copy(alpha = 0.55f)),
              contentAlignment = Alignment.Center
            ) {
              if (isFeedLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = colors.p,
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Подбираю музыку…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mu
                  )
                }
              } else {
                Text(
                  text = "Лента пока пуста.\nПроверьте интернет или источники в настройках.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = colors.mu,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 20.dp)
                )
              }
            }
          } else {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              contentPadding = PaddingValues(vertical = 4.dp)
            ) {
              itemsIndexed(recommendations, key = { _, track -> track.id }) { index, track ->
                val staggerProgress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                  if (enabledAnimations) {
                    delay(index * 50L)
                    staggerProgress.animateTo(1f, tween(550, easing = SpringEasing))
                  } else {
                    staggerProgress.snapTo(1f)
                  }
                }

                RecommendationTile(
                  track = track,
                  isCurrent = currentTrack?.id == track.id,
                  onClick = { onTrackClick(track) },
                  onLongClick = { onTrackLongClick(track) },
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
          }
        }
      }

      // Track Rows Section Header
      item {
        Text(
          text = "Все треки • ${tracks.size}",
          fontFamily = UnboundedFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 19.sp,
          color = colors.tx,
          modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
      }

      // Staggered Track Rows
      itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
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

      // Footer
      item {
        Text(
          text = "#вайбкод | riasodrochh made",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
          color = colors.mu.copy(alpha = 0.7f),
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 12.dp)
        )
      }
    }
  }
}
