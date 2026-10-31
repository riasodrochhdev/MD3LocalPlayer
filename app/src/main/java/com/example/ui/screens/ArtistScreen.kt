package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Artist
import com.example.model.Track
import com.example.ui.components.TrackRow
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.UnboundedFontFamily
import com.example.ui.theme.mediatekaColors
import com.example.ui.components.PressBox
import com.example.ui.theme.pressScale
import kotlinx.coroutines.delay

@Composable
fun ArtistScreen(
  artist: Artist,
  tracks: List<Track>,
  currentTrack: Track?,
  favIconStyle: String,
  onBackClick: () -> Unit,
  onTrackClick: (Track) -> Unit,
  onTrackLongClick: (Track) -> Unit,
  onPlayArtist: () -> Unit,
  onShuffleArtist: () -> Unit,
  onFavoriteToggle: (Long) -> Unit,
  onDownloadClick: (Track) -> Unit,
  onSelectRelatedArtist: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  var bioExpanded by remember { mutableStateOf(false) }

  val artistTracks = remember(tracks, artist) {
    tracks.filter { it.artist.contains(artist.name, ignoreCase = true) }
  }

  val popularTracks = remember(tracks) {
    tracks.take(5)
  }

  val relatedArtists = remember {
    listOf("blair", "YungMe...", "Нурминский", "Слепой", "Middle Eye")
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
      // Back Button
      item {
        PressBox(
          onClick = onBackClick,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.s1)
            .pressScale(pressedScale = 0.9f, enabled = enabledAnimations, onClick = onBackClick)
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = colors.tx,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      // Artist Hero
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(104.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(colors.pc, colors.te))),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = artist.name.take(1).uppercase(),
              fontFamily = UnboundedFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 36.sp,
              color = colors.onp
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = artist.name,
              fontFamily = UnboundedFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 26.sp,
              lineHeight = 30.sp,
              color = colors.tx
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = artist.monthlyListeners,
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
              color = colors.mu
            )
          }
        }
      }

      // Bio Text
      item {
        Text(
          text = artist.bio,
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
          color = colors.mu,
          maxLines = if (bioExpanded) Int.MAX_VALUE else 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { bioExpanded = !bioExpanded }
        )
      }

      // Action buttons
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(colors.p)
              .pressScale(
                pressedScale = 0.9f,
                durationMillis = 280,
                easing = SpringEasing,
                enabled = enabledAnimations,
                onClick = onPlayArtist
              )
              .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              tint = colors.onp,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Слушать",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              color = colors.onp
            )
          }

          Row(
            modifier = Modifier
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(colors.s1)
              .pressScale(
                pressedScale = 0.9f,
                durationMillis = 280,
                easing = SpringEasing,
                enabled = enabledAnimations,
                onClick = onShuffleArtist
              )
              .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shuffle,
              contentDescription = null,
              tint = colors.tx,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Вперемешку",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
              ),
              color = colors.tx
            )
          }
        }
      }

      // Popular Tracks
      item {
        Text(
          text = "Популярное",
          fontFamily = UnboundedFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 19.sp,
          color = colors.tx,
          modifier = Modifier.padding(top = 10.dp)
        )
      }

      itemsIndexed(popularTracks, key = { _, track -> "pop_${track.id}" }) { index, track ->
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

      // In your library tracks
      if (artistTracks.isNotEmpty()) {
        item {
          Text(
            text = "В вашей медиатеке",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            color = colors.tx,
            modifier = Modifier.padding(top = 10.dp)
          )
        }

        itemsIndexed(artistTracks, key = { _, track -> "lib_${track.id}" }) { index, track ->
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

      // Related artists rail
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Похожие исполнители",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            color = colors.tx,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            items(relatedArtists) { relName ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .width(88.dp)
                  .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                    onSelectRelatedArtist(relName)
                  }
              ) {
                Box(
                  modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.pc, colors.te))),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = relName.take(1).uppercase(),
                    fontFamily = UnboundedFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = colors.onp
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = relName,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = colors.tx,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center
                )
              }
            }
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
            .padding(top = 36.dp, bottom = 12.dp)
        )
      }
    }
  }
}
