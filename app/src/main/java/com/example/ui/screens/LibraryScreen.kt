package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.components.CoverArt
import com.example.ui.components.TrackRow
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.UnboundedFontFamily
import com.example.ui.theme.mediatekaColors
import com.example.ui.components.PressBox
import com.example.ui.theme.pressScale
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(
  tracks: List<Track>,
  currentTrack: Track?,
  currentFilter: String,
  favIconStyle: String,
  onFilterChange: (String) -> Unit,
  onTrackClick: (Track) -> Unit,
  onTrackLongClick: (Track) -> Unit,
  onFavoriteToggle: (Long) -> Unit,
  onDownloadClick: (Track) -> Unit,
  onDeleteClick: (Track) -> Unit,
  onAddMusicClick: () -> Unit,
  onAudioFilesPicked: (List<android.net.Uri>) -> Unit = {},
  onArtistClick: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  var selectedGroup: String? by remember { mutableStateOf(null) }

  val filteredTracks = remember(tracks, currentFilter, selectedGroup) {
    if (selectedGroup != null) {
      if (currentFilter == "artists") tracks.filter { it.artist == selectedGroup }
      else tracks.filter { it.album == selectedGroup }
    } else {
      when (currentFilter) {
        "fav" -> tracks.filter { it.isFavorite }
        "dl" -> tracks.filter { it.isDownloaded }
        else -> tracks
      }
    }
  }

  val filterChips = listOf(
    "all" to "Все",
    "fav" to "Избранное",
    "artists" to "Артисты",
    "albums" to "Альбомы",
    "dl" to "Загрузки"
  )

  val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      onAudioFilesPicked(uris)
    }
  }

  val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
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
          Text(
            text = "Медиатека",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
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

      // Filter Chips Row (animated background, text color & corner radius over 350ms ease)
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(filterChips) { (id, label) ->
            val selected = currentFilter == id && selectedGroup == null

            val bgAnim by animateColorAsState(
              targetValue = if (selected) colors.p else colors.s1,
              animationSpec = tween(durationMillis = if (enabledAnimations) 350 else 0, easing = EaseEasing),
              label = "chipBg"
            )
            val textColorAnim by animateColorAsState(
              targetValue = if (selected) colors.onp else colors.mu,
              animationSpec = tween(durationMillis = if (enabledAnimations) 350 else 0, easing = EaseEasing),
              label = "chipTextColor"
            )
            val cornerRadiusDp by animateDpAsState(
              targetValue = if (selected) 24.dp else 16.dp,
              animationSpec = tween(durationMillis = if (enabledAnimations) 350 else 0, easing = EaseEasing),
              label = "chipCorner"
            )

            Box(
              modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(cornerRadiusDp))
                .background(bgAnim)
                .pressScale(
                  pressedScale = 0.95f,
                  durationMillis = 280,
                  easing = SpringEasing,
                  enabled = enabledAnimations
                ) {
                  selectedGroup = null
                  onFilterChange(id)
                }
                .padding(horizontal = 18.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 15.sp
                ),
                color = textColorAnim
              )
            }
          }
        }
      }

      // Large "Добавить музыку" Card
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.s1)
            .pressScale(pressedScale = 0.96f, enabled = enabledAnimations, onClick = { filePickerLauncher.launch(arrayOf("audio/*")) })
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(colors.p),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = colors.onp,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Добавить музыку",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              ),
              color = colors.tx
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${tracks.size} треков, хранятся на устройстве и...",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = colors.mu,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Grouping Back Button
      if (selectedGroup != null) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(colors.s1)
                .clickable { selectedGroup = null }
                .padding(horizontal = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.ArrowBack,
                  contentDescription = "Back",
                  tint = colors.p,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Назад",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.tx
                )
              }
            }

            if (currentFilter == "artists") {
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .height(38.dp)
                  .clip(RoundedCornerShape(19.dp))
                  .background(colors.s1)
                  .clickable { onArtistClick(selectedGroup!!) }
                  .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Профиль артиста",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.p
                )
              }
            }
          }
        }
      }

      // Grid for Artists / Albums
      if ((currentFilter == "artists" || currentFilter == "albums") && selectedGroup == null) {
        item {
          val groups = remember(tracks, currentFilter) {
            if (currentFilter == "artists") {
              tracks.groupBy { it.artist }
            } else {
              tracks.groupBy { it.album }
            }
          }

          Column {
            groups.entries.chunked(2).forEach { rowEntries ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                rowEntries.forEach { (groupName, groupTracks) ->
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(24.dp))
                      .background(colors.s1)
                      .pressScale(pressedScale = 0.96f, enabled = enabledAnimations) {
                        selectedGroup = groupName
                      }
                      .padding(14.dp)
                  ) {
                    Column {
                      CoverArt(
                        coverHue = groupTracks.firstOrNull()?.coverHue ?: 280,
                        coverUrl = groupTracks.firstOrNull()?.coverUrl,
                        shapeType = 0,
                        modifier = Modifier
                          .fillMaxWidth()
                          .aspectRatio(1f)
                          .clip(RoundedCornerShape(20.dp))
                      )
                      Spacer(modifier = Modifier.height(10.dp))
                      Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.tx,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "${groupTracks.size} треков",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mu
                      )
                    }
                  }
                }
                if (rowEntries.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      } else {
        // List Header
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val titleText = selectedGroup ?: when (currentFilter) {
              "fav" -> "Избранное"
              "dl" -> "Загрузки"
              else -> "Все треки"
            }

            Text(
              text = "$titleText • ${filteredTracks.size}",
              fontFamily = UnboundedFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 19.sp,
              color = colors.tx
            )

            PressBox(
              onClick = {
                filteredTracks.shuffled().firstOrNull()?.let { onTrackClick(it) }
              },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.p.copy(alpha = 0.16f))
                .pressScale(pressedScale = 0.9f, enabled = enabledAnimations) {
                  filteredTracks.shuffled().firstOrNull()?.let { onTrackClick(it) }
                }
            ) {
              Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = colors.p,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }

        // Staggered Track Rows
        itemsIndexed(filteredTracks, key = { _, track -> track.id }) { index, track ->
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
            showDeleteButton = true,
            onClick = { onTrackClick(track) },
            onLongClick = { onTrackLongClick(track) },
            onFavoriteToggle = { onFavoriteToggle(track.id) },
            onDownloadClick = { onDownloadClick(track) },
            onDeleteClick = { onDeleteClick(track) },
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

      // Footer
      item {
        Text(
          text = "#вайбкод | riasodrochh made",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
          color = colors.mu.copy(alpha = 0.7f),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 12.dp)
        )
      }
    }
  }
}
