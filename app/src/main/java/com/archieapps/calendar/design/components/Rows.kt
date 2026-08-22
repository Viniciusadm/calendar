package com.archieapps.calendar.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

private val rowHeight = 56.dp
private val trackWidth = 34.dp
private val trackHeight = 18.dp

@Composable
fun Group(label: String, modifier: Modifier = Modifier, first: Boolean = false) {
    val colors = LocalChronicle.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (!first) {
            Spacer(Modifier.height(Space.xl))
            Hairline()
        }

        Spacer(Modifier.height(Space.xl))

        Text(
            text = label.uppercase(),
            style = Eyebrow,
            color = colors.slate,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(Space.xs))
    }
}

@Composable
fun ValueRow(
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalChronicle.current

    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = rowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLabel(label = label, caption = caption, tint = colors.ink, modifier = Modifier.weight(1f))

        Spacer(Modifier.width(Space.md))

        content()
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    enabled: Boolean = true,
) {
    val colors = LocalChronicle.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = rowHeight)
            .clickable(enabled = enabled, role = Role.Switch) { onToggle(!checked) }
            .semantics {
                contentDescription = label
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLabel(
            label = label,
            caption = caption,
            tint = if (enabled) colors.ink else colors.slate,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(Space.md))

        Spacer(
            Modifier.size(trackWidth, trackHeight).drawBehind {
                val radius = size.height / 2

                drawRoundRect(
                    color = if (checked) colors.brand else colors.hairline,
                    cornerRadius = CornerRadius(radius),
                    style = if (checked) Fill else DrawStroke(Stroke.nodeRing.toPx()),
                )

                drawCircle(
                    color = if (checked) colors.ground else colors.slate,
                    radius = radius * 0.56f,
                    center = Offset(if (checked) size.width - radius else radius, radius),
                )
            }
        )
    }
}

@Composable
fun NavRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    value: String? = null,
) {
    val colors = LocalChronicle.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = rowHeight)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (value == null) label else "$label, $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLabel(label = label, caption = caption, tint = colors.ink, modifier = Modifier.weight(1f))

        value?.let {
            Text(it, style = EntryMeta, color = colors.slate)
            Spacer(Modifier.width(Space.sm))
        }

        Glyph(icon = Lucide.ChevronRight, tint = colors.slate, size = Space.lg)
    }
}

@Composable
private fun RowLabel(
    label: String,
    caption: String?,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(modifier = modifier.padding(vertical = Space.sm)) {
        Text(label, style = EntryTitle, color = tint)

        caption?.let {
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = it,
                style = EntryMeta,
                color = colors.slate,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
