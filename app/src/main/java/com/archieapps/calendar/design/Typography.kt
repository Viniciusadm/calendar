package com.archieapps.calendar.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

val MonthTitle = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Light,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = (-1.4).sp,
)

val DayNumeral = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Light,
    fontSize = 19.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.4).sp,
    textAlign = TextAlign.Center,
)

val DayNumeralStrong = DayNumeral.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp)

val WeekdayLabel = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    letterSpacing = 2.2.sp,
    textAlign = TextAlign.Center,
)

val Eyebrow = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.8.sp,
)

val SheetTitle = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Light,
    fontSize = 24.sp,
    lineHeight = 29.sp,
    letterSpacing = (-0.7).sp,
)

val ButtonLabel = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.2.sp,
)

val EntryTitle = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.1).sp,
)

val EntryMeta = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
)

val EntryClock = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp,
)

internal val ChronicleTypography = Typography(
    headlineLarge = MonthTitle,
    titleMedium = EntryTitle,
    bodyMedium = EntryTitle,
    bodySmall = EntryMeta,
    labelSmall = WeekdayLabel,
)
