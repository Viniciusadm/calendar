package com.archieapps.calendar.design

import androidx.compose.ui.graphics.Color

internal val Azure = Color(0xFF349EF4)
internal val AzureDim = Color(0xFF1D6FB8)

internal val InkLight = Color(0xFF081822)
internal val SlateLight = Color(0xFF59656E)
internal val GroundLight = Color(0xFFFAFCFE)
internal val CardLight = Color(0xFFFFFFFF)
internal val MutedLight = Color(0xFFEDF3F7)
internal val HairlineLight = Color(0xFFE0E5E9)

internal val InkDark = Color(0xFFEFF2F5)
internal val SlateDark = Color(0xFF8E9AA4)
internal val GroundDark = Color(0xFF0A131A)
internal val CardDark = Color(0xFF131C23)
internal val MutedDark = Color(0xFF1C252D)
internal val HairlineDark = Color(0xFF262F37)
internal val AzureSurfaceDark = Color(0xFF163045)

internal val TokenAccent = Color(0xFFB37BFF)
internal val TokenSuccess = Color(0xFF00BC7B)
internal val TokenWarning = Color(0xFFEFA831)
internal val TokenDestructive = Color(0xFFE62B34)
internal val TokenNeutral = Color(0xFF59656E)

fun colorFromToken(hex: String?): Color? {
    val value = hex?.trim()?.removePrefix("#") ?: return null
    if (value.length != 6) return null
    val parsed = value.toLongOrNull(16) ?: return null
    return Color(parsed or 0xFF000000L)
}
