package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class MediatekaColors(
  val id: String,
  val name: String,
  val bg: Color,
  val s1: Color,
  val pc: Color,
  val p: Color,
  val onp: Color,
  val onpc: Color,
  val tx: Color,
  val mu: Color,
  val te: Color,
  val gl: Color,
  val ln: Color,
  val a1: Color,
  val a2: Color,
  val a3: Color,
  val isDark: Boolean = true
)

// Themes as defined in CSS (:root and html[data-theme=...])
val ThemeViolet = MediatekaColors(
  id = "violet",
  name = "Фиалка",
  bg = Color(0xFF201B23),
  s1 = Color(0xFF2B262F),
  pc = Color(0xFF5B3468),
  p = Color(0xFFE8B9F2),
  onp = Color(0xFF3B1D47),
  onpc = Color(0xFFF6D5FB),
  tx = Color(0xFFEDE1EC),
  mu = Color(0xFFB8ADBA),
  te = Color(0xFFF4B4AB),
  gl = Color(0x8C3A3240),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF4A2C57),
  a2 = Color(0xFF35295A),
  a3 = Color(0xFF6A3446),
  isDark = true
)

val ThemeRose = MediatekaColors(
  id = "rose",
  name = "Роза",
  bg = Color(0xFF221A1E),
  s1 = Color(0xFF2F2429),
  pc = Color(0xFF6B2E48),
  p = Color(0xFFFFB1CC),
  onp = Color(0xFF4A1230),
  onpc = Color(0xFFFFD9E5),
  tx = Color(0xFFF1DFE5),
  mu = Color(0xFFBFA9B2),
  te = Color(0xFFFFB68A),
  gl = Color(0x8C46303A),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF5A2A40),
  a2 = Color(0xFF4A2C5A),
  a3 = Color(0xFF6A4030),
  isDark = true
)

val ThemeOcean = MediatekaColors(
  id = "ocean",
  name = "Океан",
  bg = Color(0xFF121B21),
  s1 = Color(0xFF1B2830),
  pc = Color(0xFF1D4E66),
  p = Color(0xFF8FD3F4),
  onp = Color(0xFF00344A),
  onpc = Color(0xFFC8ECFF),
  tx = Color(0xFFDCEBF2),
  mu = Color(0xFFA3B8C2),
  te = Color(0xFFA8E6CF),
  gl = Color(0x8C243844),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF1B4A5E),
  a2 = Color(0xFF2A3A6A),
  a3 = Color(0xFF1C5A50),
  isDark = true
)

val ThemeMint = MediatekaColors(
  id = "mint",
  name = "Мята",
  bg = Color(0xFF131C17),
  s1 = Color(0xFF1D2822),
  pc = Color(0xFF24523A),
  p = Color(0xFF9FE0B4),
  onp = Color(0xFF00391F),
  onpc = Color(0xFFC4F5D2),
  tx = Color(0xFFDFECE3),
  mu = Color(0xFFA6B9AC),
  te = Color(0xFFE6D78A),
  gl = Color(0x8C263C30),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF1F5A3A),
  a2 = Color(0xFF1E4A52),
  a3 = Color(0xFF5A5A2A),
  isDark = true
)

val ThemeSunset = MediatekaColors(
  id = "sunset",
  name = "Закат",
  bg = Color(0xFF201915),
  s1 = Color(0xFF2D231D),
  pc = Color(0xFF6A3A1E),
  p = Color(0xFFFFB87A),
  onp = Color(0xFF4A2000),
  onpc = Color(0xFFFFDCC2),
  tx = Color(0xFFF0E0D6),
  mu = Color(0xFFBFAA9C),
  te = Color(0xFFFF9AA8),
  gl = Color(0x8C46342A),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF6A3A20),
  a2 = Color(0xFF5A2A3A),
  a3 = Color(0xFF6A4A20),
  isDark = true
)

val ThemeAmoled = MediatekaColors(
  id = "amoled",
  name = "AMOLED",
  bg = Color(0xFF000000),
  s1 = Color(0xFF131114),
  pc = Color(0xFF4A2A58),
  p = Color(0xFFE8B9F2),
  onp = Color(0xFF3B1D47),
  onpc = Color(0xFFF6D5FB),
  tx = Color(0xFFEDE1EC),
  mu = Color(0xFFB8ADBA),
  te = Color(0xFFF4B4AB),
  gl = Color(0x991A161E),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF2A1A34),
  a2 = Color(0xFF1A1A30),
  a3 = Color(0xFF301A22),
  isDark = true
)

val ThemeMono = MediatekaColors(
  id = "mono",
  name = "Моно",
  bg = Color(0xFF151515),
  s1 = Color(0xFF232323),
  pc = Color(0xFF3B3B3B),
  p = Color(0xFFF0F0F0),
  onp = Color(0xFF141414),
  onpc = Color(0xFFFAFAFA),
  tx = Color(0xFFF2F2F2),
  mu = Color(0xFFA9A9A9),
  te = Color(0xFFCFCFCF),
  gl = Color(0x8C343434),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF2C2C2C),
  a2 = Color(0xFF242424),
  a3 = Color(0xFF333333),
  isDark = true
)

val ThemeMonoBlack = MediatekaColors(
  id = "monoblack",
  name = "Моно AMOLED",
  bg = Color(0xFF000000),
  s1 = Color(0xFF121212),
  pc = Color(0xFF2E2E2E),
  p = Color(0xFFFFFFFF),
  onp = Color(0xFF000000),
  onpc = Color(0xFFFFFFFF),
  tx = Color(0xFFF5F5F5),
  mu = Color(0xFF9A9A9B),
  te = Color(0xFFCFCFCF),
  gl = Color(0x99181818),
  ln = Color(0x17FFFFFF),
  a1 = Color(0xFF1A1A1A),
  a2 = Color(0xFF141414),
  a3 = Color(0xFF202020),
  isDark = true
)

val ThemeLight = MediatekaColors(
  id = "light",
  name = "Светлая",
  bg = Color(0xFFFBF1FB),
  s1 = Color(0xFFF0E3F0),
  pc = Color(0xFFEBCDF4),
  p = Color(0xFF7A4189),
  onp = Color(0xFFFFFFFF),
  onpc = Color(0xFF3B1D47),
  tx = Color(0xFF1F1A21),
  mu = Color(0xFF6E6171),
  te = Color(0xFFA34A42),
  gl = Color(0x9EFFFFFF),
  ln = Color(0x1A3C1E46),
  a1 = Color(0xFFE8C4F3),
  a2 = Color(0xFFC9D3FF),
  a3 = Color(0xFFFFD2CC),
  isDark = false
)

val AllThemes = listOf(
  ThemeViolet,
  ThemeRose,
  ThemeOcean,
  ThemeMint,
  ThemeSunset,
  ThemeAmoled,
  ThemeMono,
  ThemeMonoBlack,
  ThemeLight
)
