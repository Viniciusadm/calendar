package com.archieapps.calendar.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.Space

private val actionHeight = 48.dp

private val actionShape = RoundedCornerShape(Space.sm)

private const val disabledAlpha = 0.4f

@Composable
fun TextAction(
    label: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = ButtonLabel,
    enabled: Boolean = true,
    stretch: Boolean = false,
    horizontal: Dp = 0.dp,
    align: Alignment = Alignment.CenterStart,
) {
    Box(
        modifier = modifier
            .then(if (stretch) Modifier.fillMaxWidth() else Modifier)
            .clip(actionShape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = actionHeight)
            .padding(horizontal = horizontal),
        contentAlignment = align,
    ) {
        Text(
            text = label,
            style = style,
            color = if (enabled) color else color.copy(alpha = disabledAlpha),
        )
    }
}
