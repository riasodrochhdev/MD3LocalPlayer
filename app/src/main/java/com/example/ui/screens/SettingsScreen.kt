package com.example.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.example.api.YmAccount
import com.example.viewmodel.YmLoginState
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SettingsState
import com.example.ui.theme.AllThemes
import com.example.ui.theme.EaseEasing
import com.example.ui.theme.SpringEasing
import com.example.ui.theme.UnboundedFontFamily
import com.example.ui.theme.mediatekaColors
import com.example.ui.components.PressBox
import com.example.ui.theme.pressScale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  settings: SettingsState,
  activeTab: String,
  onTabChange: (String) -> Unit,
  onThemeSelect: (String) -> Unit,
  onGlassSelect: (String) -> Unit,
  onSnowToggle: () -> Unit,
  onSnowAmountSelect: (Int) -> Unit,
  onFavIconSelect: (String) -> Unit,
  onVibrationToggle: () -> Unit,
  onAnimationsToggle: () -> Unit,
  onBgTypeSelect: (String) -> Unit,
  onBgColorByCoverToggle: () -> Unit,
  onBgMoveOnlyPlayingToggle: () -> Unit,
  onBgSpeedSelect: (Int) -> Unit,
  onBgIntensitySelect: (Int) -> Unit,
  onFeedLanguageSelect: (String) -> Unit,
  onAddMyArtist: (String) -> Unit,
  onRemoveMyArtist: (String) -> Unit,
  onTracksPerArtistSelect: (Int) -> Unit,
  onFeedSizeSelect: (Int) -> Unit,
  onFeedAutoPlayToggle: () -> Unit,
  onFeedAutoScrollToggle: () -> Unit,
  onDownloadDestSelect: (String) -> Unit,
  onSourceTasteToggle: () -> Unit,
  onSourceAudiusToggle: () -> Unit,
  onSourceYtToggle: () -> Unit,
  onRefreshFeed: () -> Unit,
  onResetTaste: () -> Unit,
  ymAccount: YmAccount?,
  ymError: String?,
  ymLogin: YmLoginState?,
  onYmLogin: () -> Unit,
  onYmCancelLogin: () -> Unit,
  onYmSignOut: () -> Unit,
  onYmToken: (String) -> Unit,
  onYmGateway: (String) -> Unit,
  onCobaltUrl: (String) -> Unit,
  onCobaltKey: (String) -> Unit,
  onYtWorker: (String) -> Unit,
  onPipedUrl: (String) -> Unit,
  onYtApiKey: (String) -> Unit,
  onShowToast: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors
  var newArtistInput by remember { mutableStateOf("") }
  var prompt by remember { mutableStateOf<PromptSpec?>(null) }
  val uriHandler = LocalUriHandler.current
  val clipboard = LocalClipboardManager.current

  val tabs = listOf(
    "look" to "Вид",
    "bg" to "Фон",
    "feed" to "Лента",
    "svc" to "Сервисы"
  )

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
          text = "Настройки",
          fontFamily = UnboundedFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 32.sp,
          color = colors.tx
        )
      }

      // 4 Tab Pills Switcher
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.s1.copy(alpha = 0.5f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          tabs.forEach { (id, label) ->
            val selected = activeTab == id
            val tabBgAnim by animateColorAsState(
              targetValue = if (selected) colors.p else Color.Transparent,
              animationSpec = tween(durationMillis = if (enabledAnimations) 350 else 0, easing = EaseEasing),
              label = "settingsTabBg"
            )
            val tabTextColorAnim by animateColorAsState(
              targetValue = if (selected) colors.onp else colors.mu,
              animationSpec = tween(durationMillis = if (enabledAnimations) 350 else 0, easing = EaseEasing),
              label = "settingsTabTextColor"
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tabBgAnim)
                .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                  onTabChange(id)
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 14.sp
                ),
                color = tabTextColorAnim
              )
            }
          }
        }
      }

      // TAB 1: "Вид"
      if (activeTab == "look") {
        item {
          SettingsCard(title = "Тема") {
            Column {
              val themeRows = AllThemes.chunked(4)
              themeRows.forEach { rowThemes ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceAround
                ) {
                  rowThemes.forEach { theme ->
                    val selected = theme.id == settings.themeId
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier
                        .pressScale(pressedScale = 0.9f, enabled = enabledAnimations) {
                          onThemeSelect(theme.id)
                        }
                    ) {
                      Box(
                        modifier = Modifier
                          .size(52.dp)
                          .clip(CircleShape)
                          .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) colors.p else colors.ln,
                            shape = CircleShape
                          )
                          .background(
                            Brush.linearGradient(
                              0.0f to theme.bg,
                              0.5f to theme.bg,
                              0.5f to theme.p,
                              1.0f to theme.p
                            )
                          )
                      )
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontSize = 12.sp,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (selected) colors.tx else colors.mu
                      )
                    }
                  }
                  repeat(4 - rowThemes.size) {
                    Spacer(modifier = Modifier.size(52.dp))
                  }
                }
              }
            }
          }
        }

        item {
          SettingsCard(title = "Стекло") {
            SegmentedPicker(
              options = listOf(
                "off" to "Без размытия",
                "blur" to "Обычное размытие",
                "liquid" to "Liquid Glass"
              ),
              selected = settings.glassStyle,
              onSelect = onGlassSelect,
              enabledAnimations = enabledAnimations
            )
          }
        }

        item {
          SettingsCard(title = "Снежинки") {
            Column {
              SettingsToggleRow(
                title = "Падающий снег",
                subtitle = "Лёгкая анимация поверх всего приложения",
                checked = settings.snowEnabled,
                onCheckedChange = { onSnowToggle() }
              )

              if (settings.snowEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedPicker(
                  options = listOf("1" to "Мало", "2" to "Средне", "3" to "Много"),
                  selected = settings.snowAmount.toString(),
                  onSelect = { onSnowAmountSelect(it.toInt()) },
                  enabledAnimations = enabledAnimations
                )
              }
            }
          }
        }

        item {
          SettingsCard(title = "Значок «в избранное»") {
            SegmentedPicker(
              options = listOf("plus" to "Плюс", "heart" to "Сердце"),
              selected = settings.favIcon,
              onSelect = onFavIconSelect,
              enabledAnimations = enabledAnimations
            )
          }
        }

        item {
          SettingsCard(title = "Отклик и анимации") {
            Column {
              SettingsToggleRow(
                title = "Вибрация",
                subtitle = "Лёгкий отклик на нажатия, свайпы и долгое нажатие",
                checked = settings.vibrationEnabled,
                onCheckedChange = { onVibrationToggle() }
              )

              Spacer(modifier = Modifier.height(12.dp))

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .clip(RoundedCornerShape(24.dp))
                  .background(colors.s1)
                  .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                    onAnimationsToggle()
                  },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (settings.alwaysAnimations) "Анимации: всегда включены" else "Анимации: как в системе",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                  ),
                  color = colors.tx
                )
              }
            }
          }
        }
      }

      // TAB 2: "Фон"
      if (activeTab == "bg") {
        item {
          SettingsCard(
            title = "Анимированный фон",
            subtitle = "Фон виден за этими карточками, поэтому изменения заметны сразу."
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              SegmentedPicker(
                options = listOf("off" to "Статичный", "fluid" to "Пятна", "waves" to "Волны"),
                selected = settings.bgType,
                onSelect = onBgTypeSelect,
                enabledAnimations = enabledAnimations
              )

              SettingsToggleRow(
                title = "Цвета по обложке",
                subtitle = "Фон подстраивается под играющий трек",
                checked = settings.bgColorByCover,
                onCheckedChange = { onBgColorByCoverToggle() }
              )

              SettingsToggleRow(
                title = "Двигать только во время музыки",
                subtitle = "На паузе фон замирает почти полностью",
                checked = settings.bgMoveOnlyWhilePlaying,
                onCheckedChange = { onBgMoveOnlyPlayingToggle() }
              )

              Text(
                text = "Скорость",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = colors.mu
              )

              SegmentedPicker(
                options = listOf("1" to "Медленно", "2" to "Средне", "3" to "Быстро"),
                selected = settings.bgSpeed.toString(),
                onSelect = { onBgSpeedSelect(it.toInt()) },
                enabledAnimations = enabledAnimations
              )

              Text(
                text = "Насыщенность",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = colors.mu
              )

              SegmentedPicker(
                options = listOf("1" to "Мягко", "2" to "Средне", "3" to "Ярко"),
                selected = settings.bgIntensity.toString(),
                onSelect = { onBgIntensitySelect(it.toInt()) },
                enabledAnimations = enabledAnimations
              )
            }
          }
        }
      }

      // TAB 3: "Лента"
      if (activeTab == "feed") {
        item {
          SettingsCard(
            title = "Как строится лента",
            subtitle = "Главный источник — ваше избранное: берутся похожие на него треки и связанные исполнители. Ниже можно добавить свои исполнители и жанры."
          ) {}
        }

        item {
          SettingsCard(
            title = "Язык треков",
            subtitle = "«По вкусу» сам выбирает язык по вашему избранному."
          ) {
            SegmentedPicker(
              options = listOf(
                "fav" to "По вкусу",
                "any" to "Любой",
                "pref" to "Больше русских",
                "ru" to "Только русские"
              ),
              selected = settings.feedLanguage,
              onSelect = onFeedLanguageSelect,
              enabledAnimations = enabledAnimations
            )
          }
        }

        item {
          SettingsCard(title = "Мои исполнители") {
            Column {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
              ) {
                settings.myArtists.forEach { artistName ->
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(18.dp))
                      .background(colors.p)
                      .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                        onRemoveMyArtist(artistName)
                      }
                      .padding(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.onp
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = colors.onp,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                  value = newArtistInput,
                  onValueChange = { newArtistInput = it },
                  placeholder = { Text("Например: Кино", color = colors.mu) },
                  singleLine = true,
                  shape = RoundedCornerShape(22.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.s1,
                    unfocusedContainerColor = colors.s1,
                    focusedBorderColor = colors.p,
                    unfocusedBorderColor = colors.ln,
                    focusedTextColor = colors.tx,
                    unfocusedTextColor = colors.tx
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                PressBox(
                  onClick = {
                    if (newArtistInput.isNotBlank()) {
                      onAddMyArtist(newArtistInput)
                      newArtistInput = ""
                    }
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.p)
                    .pressScale(pressedScale = 0.9f, enabled = enabledAnimations) {
                      if (newArtistInput.isNotBlank()) {
                        onAddMyArtist(newArtistInput)
                        newArtistInput = ""
                      }
                    }
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = colors.onp,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
            }
          }
        }

        item {
          SettingsCard(title = "Объём") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(text = "Треков одного исполнителя", style = MaterialTheme.typography.bodySmall, color = colors.mu)
              SegmentedPicker(
                options = listOf("1" to "1", "2" to "2", "3" to "3", "5" to "5"),
                selected = settings.tracksPerArtist.toString(),
                onSelect = { onTracksPerArtistSelect(it.toInt()) },
                enabledAnimations = enabledAnimations
              )

              Text(text = "Размер ленты", style = MaterialTheme.typography.bodySmall, color = colors.mu)
              SegmentedPicker(
                options = listOf("10" to "10", "20" to "20", "30" to "30"),
                selected = settings.feedSize.toString(),
                onSelect = { onFeedSizeSelect(it.toInt()) },
                enabledAnimations = enabledAnimations
              )
            }
          }
        }

        item {
          SettingsCard(title = "Источники и поведение") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              SettingsToggleRow(
                title = "Учитывать прослушивания",
                subtitle = "Если избранного мало",
                checked = settings.sourceTaste,
                onCheckedChange = { onSourceTasteToggle() }
              )
              SettingsToggleRow(
                title = "Audius",
                subtitle = null,
                checked = settings.sourceAudius,
                onCheckedChange = { onSourceAudiusToggle() }
              )
              SettingsToggleRow(
                title = "YouTube Music",
                subtitle = "Нужен рабочий Piped или свой воркер",
                checked = settings.sourceYt,
                onCheckedChange = { onSourceYtToggle() }
              )
              SettingsToggleRow(
                title = "Играть дальше по ленте",
                subtitle = "После трека включается следующий из ленты",
                checked = settings.feedAutoPlay,
                onCheckedChange = { onFeedAutoPlayToggle() }
              )
              SettingsToggleRow(
                title = "Листать ленту самой",
                subtitle = "Каждые несколько секунд",
                checked = settings.feedAutoScroll,
                onCheckedChange = { onFeedAutoScrollToggle() }
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.s1)
                    .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                      onRefreshFeed()
                      onShowToast("Обновляю ленту…")
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(text = "Обновить ленту", style = MaterialTheme.typography.titleSmall, color = colors.tx)
                }
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.s1)
                    .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
                      onResetTaste()
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(text = "Сбросить мой вкус", style = MaterialTheme.typography.titleSmall, color = colors.tx)
                }
              }
            }
          }
        }
      }

      // TAB 4: "Сервисы"
      if (activeTab == "svc") {
        item {
          SettingsCard(
            title = "Яндекс Музыка",
            subtitle = "Поиск, похожие треки и скачивание через ваш шлюз (Cloudflare Worker). Неофициальный API."
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              val statusText = when {
                ymLogin != null -> "Ждём подтверждения. Код: ${ymLogin.code}"
                ymAccount != null -> "Подключено: ${ymAccount.login}" +
                  (if (ymAccount.plus) " · Плюс" else " · без Плюса (треки по 30 секунд)")
                ymError != null -> "Ошибка: $ymError"
                settings.ymGateway.isBlank() -> "Не подключено: сначала укажите адрес шлюза"
                settings.ymToken.isBlank() -> "Шлюз указан. Осталось войти или вставить токен"
                else -> "Проверяю подключение…"
              }
              val statusColor = when {
                ymAccount != null || ymLogin != null -> colors.p
                ymError != null -> Color(0xFFE5484D)
                else -> colors.mu
              }
              Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = statusColor
              )

              if (ymLogin != null) {
                Text(
                  text = "Откройте страницу ${ymLogin.url}, войдите в Яндекс и введите код.",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.mu
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  ActionPill(
                    text = "Открыть страницу",
                    filled = true,
                    enabledAnimations = enabledAnimations,
                    modifier = Modifier.weight(1f)
                  ) {
                    try {
                      uriHandler.openUri(ymLogin.url)
                    } catch (_: Exception) {
                      onShowToast("Не удалось открыть браузер")
                    }
                  }
                  ActionPill(
                    text = "Скопировать код",
                    enabledAnimations = enabledAnimations,
                    modifier = Modifier.weight(1f)
                  ) {
                    clipboard.setText(AnnotatedString(ymLogin.code))
                    onShowToast("Код скопирован")
                  }
                }
                ActionPill(text = "Отмена", enabledAnimations = enabledAnimations) { onYmCancelLogin() }
              }

              ServiceButton(
                title = "Адрес шлюза",
                value = settings.ymGateway.ifBlank { "не задан" },
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Адрес шлюза",
                  placeholder = "https://имя.аккаунт.workers.dev",
                  hint = "Адрес вашего Cloudflare Worker с Яндекс-шлюзом. Токен будет уходить на этот адрес, поэтому указывайте только свой. Пустое поле отключает шлюз.",
                  initial = settings.ymGateway,
                  onConfirm = onYmGateway
                )
              }

              if (ymLogin == null) {
                ActionPill(text = "Войти через Яндекс", filled = true, enabledAnimations = enabledAnimations) {
                  onYmLogin()
                }
              }

              ServiceButton(
                title = "Вставить токен",
                value = maskSecret(settings.ymToken),
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Токен Яндекс Музыки",
                  placeholder = "y0_AgAAAA…",
                  hint = "OAuth-токен аккаунта. Обычно проще войти по коду кнопкой выше.",
                  initial = "",
                  onConfirm = onYmToken
                )
              }

              if (settings.ymToken.isNotBlank()) {
                ActionPill(text = "Выйти из аккаунта", enabledAnimations = enabledAnimations) { onYmSignOut() }
              }
            }
          }
        }

        item {
          SettingsCard(
            title = "Audius",
            subtitle = "Бесплатный открытый каталог музыки, играет через встроенный плеер без ключей и авторизации."
          ) {
            Text(
              text = if (settings.sourceAudius) "Подключено и работает по умолчанию" else "Отключено в ленте (поиск всё равно ищет и здесь)",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = colors.p
            )
          }
        }

        item {
          SettingsCard(
            title = "Скачивание",
            subtitle = "В медиатеке трек играет без интернета и лежит в разделе «Загрузки». Файл сохраняется в загрузки телефона (Android 10+)."
          ) {
            SegmentedPicker(
              options = listOf("both" to "Везде", "lib" to "В медиатеку", "file" to "Файлом"),
              selected = settings.downloadDestination,
              onSelect = onDownloadDestSelect,
              enabledAnimations = enabledAnimations
            )
          }
        }

        item {
          SettingsCard(
            title = "Cobalt для YouTube (необязательно)",
            subtitle = "Свой сервер Cobalt. Если задан, звук YouTube берётся сначала оттуда."
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ServiceButton(
                title = "Адрес Cobalt",
                value = settings.cobaltUrl.ifBlank { "не задан" },
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Адрес Cobalt",
                  placeholder = "https://cobalt.example.com",
                  hint = "Пустое поле отключает Cobalt.",
                  initial = settings.cobaltUrl,
                  onConfirm = onCobaltUrl
                )
              }
              ServiceButton(
                title = "Ключ API",
                value = maskSecret(settings.cobaltKey),
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Ключ Cobalt API",
                  placeholder = "ключ",
                  hint = "Нужен, только если ваш Cobalt защищён ключом.",
                  initial = "",
                  onConfirm = onCobaltKey
                )
              }
            }
          }
        }

        item {
          SettingsCard(
            title = "Поиск по YouTube",
            subtitle = "По умолчанию используются публичные серверы Piped. Если они не отвечают, укажите свой сервер Piped, свой воркер или ключ YouTube Data API."
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ServiceButton(
                title = "Свой воркер",
                value = settings.ytWorker.ifBlank { "не задан" },
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Свой воркер для поиска",
                  placeholder = "https://имя.аккаунт.workers.dev",
                  hint = "Должен отвечать на GET /?q=запрос списком [{id, t, c, d}]. Пустое поле отключает воркер.",
                  initial = settings.ytWorker,
                  onConfirm = onYtWorker
                )
              }
              ServiceButton(
                title = "Сервер Piped",
                value = settings.pipedUrl.ifBlank { "публичные" },
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Сервер Piped",
                  placeholder = "https://pipedapi.example.com",
                  hint = "Адрес API (не сайта) вашего Piped. Он будет опрашиваться первым. Пустое поле вернёт публичные серверы.",
                  initial = settings.pipedUrl,
                  onConfirm = onPipedUrl
                )
              }
              ServiceButton(
                title = "Ключ YouTube API",
                value = maskSecret(settings.ytApiKey),
                enabledAnimations = enabledAnimations
              ) {
                prompt = PromptSpec(
                  title = "Ключ YouTube Data API",
                  placeholder = "AIza…",
                  hint = "Используется для поиска, если Piped и воркер не ответили.",
                  initial = "",
                  onConfirm = onYtApiKey
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
            .padding(top = 40.dp, bottom = 12.dp)
        )
      }
    }
  }

  prompt?.let { spec ->
    PromptDialog(spec = spec, onDismiss = { prompt = null })
  }
}

@Composable
private fun SettingsCard(
  title: String,
  subtitle: String? = null,
  content: @Composable () -> Unit
) {
  val colors = MaterialTheme.mediatekaColors

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(28.dp))
      .background(colors.s1.copy(alpha = 0.74f))
      .border(1.dp, colors.ln, RoundedCornerShape(28.dp))
      .padding(18.dp)
  ) {
    Text(
      text = title,
      fontFamily = UnboundedFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 17.sp,
      color = colors.tx
    )

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
        color = colors.mu
      )
    }

    Spacer(modifier = Modifier.height(12.dp))
    content()
  }
}

@Composable
private fun SegmentedPicker(
  options: List<Pair<String, String>>,
  selected: String,
  onSelect: (String) -> Unit,
  enabledAnimations: Boolean = true
) {
  val colors = MaterialTheme.mediatekaColors

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(colors.s1)
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    options.forEach { (key, label) ->
      val isSelected = key == selected
      val bgAnim by animateColorAsState(
        targetValue = if (isSelected) colors.p else Color.Transparent,
        animationSpec = tween(durationMillis = if (enabledAnimations) 300 else 0, easing = EaseEasing),
        label = "segmentBg"
      )
      val textColorAnim by animateColorAsState(
        targetValue = if (isSelected) colors.onp else colors.mu,
        animationSpec = tween(durationMillis = if (enabledAnimations) 300 else 0, easing = EaseEasing),
        label = "segmentTextColor"
      )

      Box(
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(bgAnim)
          .pressScale(pressedScale = 0.95f, enabled = enabledAnimations) {
            onSelect(key)
          },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
          ),
          color = textColorAnim,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  subtitle: String?,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  val colors = MaterialTheme.mediatekaColors

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp
        ),
        color = colors.tx
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = colors.mu
        )
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = colors.onp,
        checkedTrackColor = colors.p,
        uncheckedThumbColor = colors.mu,
        uncheckedTrackColor = colors.tx.copy(alpha = 0.16f)
      )
    )
  }
}


private data class PromptSpec(
  val title: String,
  val placeholder: String,
  val hint: String,
  val initial: String,
  val onConfirm: (String) -> Unit
)

private fun maskSecret(value: String): String =
  if (value.isBlank()) "не задан" else "•••• " + value.takeLast(4)

@Composable
private fun PromptDialog(spec: PromptSpec, onDismiss: () -> Unit) {
  val colors = MaterialTheme.mediatekaColors
  var text by remember(spec) { mutableStateOf(spec.initial) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.s1,
    titleContentColor = colors.tx,
    textContentColor = colors.mu,
    title = { Text(text = spec.title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (spec.hint.isNotEmpty()) {
          Text(text = spec.hint, style = MaterialTheme.typography.bodySmall, color = colors.mu)
        }
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          singleLine = true,
          placeholder = { Text(text = spec.placeholder, color = colors.mu.copy(alpha = 0.6f)) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.p,
            unfocusedBorderColor = colors.ln,
            focusedTextColor = colors.tx,
            unfocusedTextColor = colors.tx,
            cursorColor = colors.p
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      TextButton(onClick = {
        spec.onConfirm(text)
        onDismiss()
      }) {
        Text(text = "Сохранить", color = colors.p, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = "Отмена", color = colors.mu)
      }
    }
  )
}

/** Кнопка-строка: название и текущее значение, по нажатию открывается ввод. */
@Composable
private fun ServiceButton(
  title: String,
  value: String,
  enabledAnimations: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.mediatekaColors
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(colors.s1)
      .pressScale(pressedScale = 0.97f, enabled = enabledAnimations, onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 11.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      color = colors.tx
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = colors.mu,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ActionPill(
  text: String,
  enabledAnimations: Boolean,
  modifier: Modifier = Modifier,
  filled: Boolean = false,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.mediatekaColors
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(if (filled) colors.p else colors.s1)
      .pressScale(pressedScale = 0.95f, enabled = enabledAnimations, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = if (filled) colors.onp else colors.tx
    )
  }
}
