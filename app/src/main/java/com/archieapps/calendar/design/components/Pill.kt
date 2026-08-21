package com.archieapps.calendar.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space

private val pillShape = RoundedCornerShape(percent = 50)

@Composable
fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dot: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(if (selected) colors.brandSoft else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) colors.brand.copy(alpha = 0.55f) else colors.hairline,
                shape = pillShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(Space.sm))
        }

        Text(
            text = label,
            style = EntryMeta,
            color = if (selected) colors.brand else colors.slate,
        )
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
