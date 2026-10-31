package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OnestFontFamily = FontFamily(
  Font(R.font.onest, FontWeight.Normal),
  Font(R.font.onest, FontWeight.Medium),
  Font(R.font.onest, FontWeight.SemiBold),
  Font(R.font.onest, FontWeight.Bold)
)

val UnboundedFontFamily = FontFamily(
  Font(R.font.unbounded, FontWeight.Normal),
  Font(R.font.unbounded, FontWeight.Medium),
  Font(R.font.unbounded, FontWeight.SemiBold),
  Font(R.font.unbounded, FontWeight.Bold)
)

val AppTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = UnboundedFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 38.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.03).sp
  ),
  displayMedium = TextStyle(
    fontFamily = UnboundedFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.03).sp
  ),
  displaySmall = TextStyle(
    fontFamily = UnboundedFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.02).sp
  ),
  headlineLarge = TextStyle(
    fontFamily = UnboundedFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.02).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = UnboundedFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 21.sp,
    letterSpacing = (-0.01).sp
  ),
  headlineSmall = TextStyle(
    fontFamily = UnboundedFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 19.sp
  ),
  titleLarge = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 22.sp
  ),
  titleMedium = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp
  ),
  titleSmall = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 18.sp
  ),
  bodySmall = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
  ),
  labelLarge = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp
  ),
  labelMedium = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
  ),
  labelSmall = TextStyle(
    fontFamily = OnestFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp
  )
)
