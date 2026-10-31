package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.mediatekaColors
import com.example.ui.theme.pressScale
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.roundToInt

data class NavItem(
  val id: String,
  val label: String,
  val icon: ImageVector
)

/**
 * Плавающая панель навигации.
 *
 * Анимация переделана: раньше подпись появлялась через AnimatedVisibility без раскрытия по ширине,
 * и панель «прыгала». Теперь всем управляет одно число pos (дробный индекс вкладки):
 *  - таблетка выбранной вкладки плавно переезжает и растягивается между вкладками;
 *  - подпись раскрывается по ширине ровно настолько, насколько вкладка «выбрана»;
 *  - цвета иконок перетекают, а не переключаются.
 */
@Composable
fun BottomNavBar(
  currentTab: String,
  onTabSelected: (String) -> Unit,
  glassStyle: String = "blur", // "off", "blur", "liquid"
  coverHue: Float = 260f,
  hazeState: HazeState? = null,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  val items = remember {
    listOf(
      NavItem("home", "Главная", Icons.Default.Home),
      NavItem("search", "Поиск", Icons.Default.Search),
      NavItem("lib", "Медиатека", Icons.Default.LibraryMusic),
      NavItem("settings", "Настройки", Icons.Default.Tune)
    )
  }
  val selectedIndex = items.indexOfFirst { it.id == currentTab }.coerceAtLeast(0)

  // появление панели при запуске
  val entranceProgress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    if (enabledAnimations) {
      entranceProgress.animateTo(1f, tween(durationMillis = 600, easing = SpringEasing))
    } else {
      entranceProgress.snapTo(1f)
    }
  }

  // дробный индекс выбранной вкладки: единственный источник анимации
  val pos = remember { Animatable(selectedIndex.toFloat()) }
  LaunchedEffect(selectedIndex, enabledAnimations) {
    if (enabledAnimations) {
      pos.animateTo(selectedIndex.toFloat(), tween(durationMillis = 460, easing = EaseEasing))
    } else {
      pos.snapTo(selectedIndex.toFloat())
    }
  }

  // оттенок стекла от обложки играющего трека
  val targetTrackTint = remember(coverHue) {
    Color.hsl(hue = (coverHue % 360f + 360f) % 360f, saturation = 0.55f, lightness = 0.28f)
  }
  val animatedTrackTint by animateColorAsState(
    targetValue = targetTrackTint,
    animationSpec = tween(durationMillis = 600, easing = EaseEasing),
    label = "navTrackTint"
  )

  Box(
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .graphicsLayer {
        val p = entranceProgress.value
        alpha = p
        scaleX = 0.9f + (0.1f * p)
        scaleY = 0.9f + (0.1f * p)
        translationY = (1f - p) * 30.dp.toPx()
      },
    contentAlignment = Alignment.Center
  ) {
    GlassSurface(
      style = glassStyle,
      hazeState = hazeState,
      tint = animatedTrackTint,
      cornerRadius = 36.dp,
      solidColor = colors.s1
    ) {
      SlidingNavRow(
        items = items,
        pos = pos,
        isLiquid = glassStyle == "liquid",
        onTabSelected = onTabSelected,
        enabledAnimations = enabledAnimations,
        modifier = Modifier.padding(6.dp)
      )
    }
  }
}

private fun mix(a: Int, b: Int, f: Float): Int = (a + (b - a) * f).roundToInt()

@Composable
private fun SlidingNavRow(
  items: List<NavItem>,
  pos: Animatable<Float, AnimationVector1D>,
  isLiquid: Boolean,
  onTabSelected: (String) -> Unit,
  enabledAnimations: Boolean,
  modifier: Modifier = Modifier
) {
  Layout(
    modifier = modifier,
    content = {
      // первый ребёнок: таблетка выбранной вкладки (рисуется под вкладками)
      NavPill(pos = pos, isLiquid = isLiquid)
      items.forEachIndexed { index, item ->
        NavTab(
          item = item,
          index = index,
          pos = pos,
          isLiquid = isLiquid,
          onClick = { onTabSelected(item.id) },
          enabledAnimations = enabledAnimations
        )
      }
    }
  ) { measurables, _ ->
    val gap = 6.dp.roundToPx()
    val tabs = measurables.drop(1).map { it.measure(Constraints()) }
    val height = tabs.maxOf { it.height }

    val xs = IntArray(tabs.size)
    var x = 0
    for (i in tabs.indices) {
      xs[i] = x
      x += tabs[i].width
      if (i < tabs.lastIndex) x += gap
    }
    val totalWidth = x

    // таблетка плавно переезжает от одной вкладки к другой и растягивается по ширине
    val p = pos.value.coerceIn(0f, (tabs.size - 1).toFloat())
    val lo = p.toInt().coerceIn(0, tabs.size - 1)
    val hi = (lo + 1).coerceAtMost(tabs.size - 1)
    val f = p - lo
    val left = mix(xs[lo], xs[hi], f)
    val right = mix(xs[lo] + tabs[lo].width, xs[hi] + tabs[hi].width, f)
    val pill = measurables[0].measure(Constraints.fixed((right - left).coerceAtLeast(0), height))

    layout(totalWidth, height) {
      pill.place(left, 0)
      tabs.forEachIndexed { i, t -> t.place(xs[i], 0) }
    }
  }
}

@Composable
private fun NavPill(
  pos: Animatable<Float, AnimationVector1D>,
  isLiquid: Boolean
) {
  val colors = MaterialTheme.mediatekaColors
  val shape = RoundedCornerShape(28.dp)

  val look = if (isLiquid) {
    Modifier
      .clip(shape)
      .background(
        Brush.linearGradient(
          0f to Color(0x4DFFFFFF),
          1f to Color(0x1FFFFFFF)
        )
      )
      .border(1.dp, Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x1AFFFFFF))), shape)
  } else {
    Modifier
      .clip(shape)
      .background(colors.pc)
  }

  Box(
    modifier = Modifier
      .graphicsLayer {
        // как в прототипе (.np.mv): во время движения таблетка чуть вытягивается и сплющивается
        val moving = (abs(pos.value - pos.value.roundToInt()) * 2f).coerceIn(0f, 1f)
        scaleX = 1f + 0.06f * moving
        scaleY = 1f - 0.08f * moving
      }
      .then(look)
  )
}

@Composable
private fun NavTab(
  item: NavItem,
  index: Int,
  pos: Animatable<Float, AnimationVector1D>,
  isLiquid: Boolean,
  onClick: () -> Unit,
  enabledAnimations: Boolean
) {
  val colors = MaterialTheme.mediatekaColors

  // 0 = вкладка свёрнута, 1 = выбрана
  val progress = (1f - abs(pos.value - index)).coerceIn(0f, 1f)
  val selectedColor = if (isLiquid) colors.tx else colors.onpc
  val contentColor = lerp(colors.mu, selectedColor, progress)

  Layout(
    modifier = Modifier
      .testTag("nav_tab_${item.id}")
      .height(56.dp)
      .clip(RoundedCornerShape(28.dp))
      .pressScale(
        pressedScale = 0.92f,
        durationMillis = 300,
        easing = SpringEasing,
        enabled = enabledAnimations,
        onClick = onClick
      ),
    content = {
      Row(
        modifier = Modifier.padding(start = 16.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = item.icon,
          contentDescription = item.label,
          tint = contentColor,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = item.label,
          color = contentColor.copy(alpha = progress),
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          ),
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
    }
  ) { measurables, _ ->
    val content = measurables[0].measure(Constraints())
    val collapsed = 56.dp.roundToPx()
    val h = 56.dp.roundToPx()
    // ширина вкладки растёт вместе с «выбранностью»: подпись раскрывается, а не появляется рывком
    val prog = (1f - abs(pos.value - index)).coerceIn(0f, 1f)
    val w = (collapsed + (content.width - collapsed).coerceAtLeast(0) * prog).roundToInt()
    layout(w, h) {
      content.place(0, (h - content.height) / 2)
    }
  }
}
