package com.archieapps.calendar.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space

private val pillShape = RoundedCornerShape(Space.md)

private const val borderAlpha = 0.55f

@Composable
fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dot: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    PillBody(
        label = label,
        labelColor = if (selected) colors.brand else colors.slate,
        borderColor = if (selected) colors.brand.copy(alpha = borderAlpha) else colors.hairline,
        background = if (selected) colors.brandSoft else colors.surface,
        dot = dot,
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
    )
}

@Composable
fun Pill(
    label: String,
    onClick: () -> Unit,
    tint: Color,
    dot: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    PillBody(
        label = label,
        labelColor = tint,
        borderColor = tint.copy(alpha = borderAlpha),
        background = colors.surface,
        dot = dot,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun PillBody(
    label: String,
    labelColor: Color,
    borderColor: Color,
    background: Color,
    dot: Color?,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .clip(pillShape)
            .background(background)
            .border(width = 1.dp, color = borderColor, shape = pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(Space.sm))
        }

        Text(text = label, style = EntryMeta, color = labelColor)
    }
}

@Composable
fun ScrollingPills(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
fun PillRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() },
    )
}
