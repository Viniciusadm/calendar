package com.archieapps.calendar.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ChronicleColors(
    val ground: Color,
    val surface: Color,
    val ink: Color,
    val slate: Color,
    val hairline: Color,
    val muted: Color,
    val brand: Color,
    val brandSoft: Color,
    val destructive: Color,
    val isDark: Boolean,
)

val LocalChronicle: ProvidableCompositionLocal<ChronicleColors> = staticCompositionLocalOf {
    error("ChronicleColors nao foi fornecido")
}

private val lightChronicle = ChronicleColors(
    ground = GroundLight,
    surface = CardLight,
    ink = InkLight,
    slate = SlateLight,
    hairline = HairlineLight,
    muted = MutedLight,
    brand = Azure,
    brandSoft = Color(0x1A349EF4),
    destructive = TokenDestructive,
    isDark = false,
)

private val darkChronicle = ChronicleColors(
    ground = GroundDark,
    surface = CardDark,
    ink = InkDark,
    slate = SlateDark,
    hairline = HairlineDark,
    muted = MutedDark,
    brand = Azure,
    brandSoft = AzureSurfaceDark,
    destructive = TokenDestructiveDark,
    isDark = true,
)

fun chronicleColors(dark: Boolean): ChronicleColors = if (dark) darkChronicle else lightChronicle

private val lightScheme = lightColorScheme(
    primary = Azure,
    onPrimary = Color.White,
    secondary = AzureDim,
    background = GroundLight,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = MutedLight,
    onSurfaceVariant = SlateLight,
    outline = HairlineLight,
    error = TokenDestructive,
)

private val darkScheme = darkColorScheme(
    primary = Azure,
    onPrimary = Color(0xFF04131F),
    secondary = TokenAccent,
    background = GroundDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = MutedDark,
    onSurfaceVariant = SlateDark,
    outline = HairlineDark,
    error = TokenDestructive,
)

enum class ThemeMode { System, Light, Dark }

@Composable
fun CalendarTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val chronicle = if (darkTheme) darkChronicle else lightChronicle

    CompositionLocalProvider(
        LocalChronicle provides chronicle,
        LocalContentColor provides chronicle.ink,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme else lightScheme,
            typography = ChronicleTypography,
            content = content,
        )
    }
}
