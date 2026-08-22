package com.archieapps.calendar.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

@Composable
fun Glyph(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun CircleButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 40,
    tint: Color? = null,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(icon = icon, tint = tint ?: colors.slate, size = (diameter * 0.45f).dp)
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 56,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(colors.brand)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(
            icon = icon,
            tint = if (colors.isDark) colors.ground else Color.White,
            size = (diameter * 0.44f).dp,
        )
    }
}

@Composable
fun SearchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 36,
) {
    CircleButton(
        icon = Lucide.Search,
        label = label,
        onClick = onClick,
        modifier = modifier,
        diameter = diameter,
    )
}

@Composable
fun Avatar(
    initial: String,
    label: String,
    photo: ImageBitmap?,
    modifier: Modifier = Modifier,
    diameter: Int = 36,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .border(1.dp, colors.hairline, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(initial, style = Eyebrow, color = colors.slate)
        }
    }
}
