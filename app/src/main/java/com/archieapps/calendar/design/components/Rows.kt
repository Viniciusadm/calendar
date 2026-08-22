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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke

private val rowHeight = 52.dp
private val trackWidth = 34.dp
private val trackHeight = 18.dp

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
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = EntryTitle,
                color = if (enabled) colors.ink else colors.slate,
            )

            caption?.let {
                Spacer(Modifier.height(Space.xxs))
                Text(it, style = EntryMeta, color = colors.slate)
            }
        }

        Spacer(Modifier.width(Space.md))

        Spacer(
            Modifier.size(trackWidth, trackHeight).drawBehind {
                val radius = size.height / 2
                val track = if (checked) colors.brand else colors.hairline

                drawRoundRect(
                    color = track,
                    cornerRadius = CornerRadius(radius),
                    style = if (checked) Fill else DrawStroke(Stroke.nodeRing.toPx()),
                )

                val knobX = if (checked) size.width - radius else radius

                drawCircle(
                    color = if (checked) colors.ground else colors.slate,
                    radius = radius * 0.58f,
                    center = Offset(knobX, radius),
                )
            }
        )
    }
}

@Composable
fun ChoiceRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = Space.sm)) {
        Section(label)

        ScrollingPills {
            options.forEach { (value, text) ->
                Pill(label = text, selected = selected == value, onClick = { onSelect(value) })
            }
        }
    }
}

@Composable
fun NavRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        Text(label, style = EntryTitle, color = colors.ink, modifier = Modifier.weight(1f))

        value?.let {
            Text(it, style = EntryMeta, color = colors.slate)
            Spacer(Modifier.width(Space.sm))
        }

        Spacer(
            Modifier.size(Space.md, Space.md).drawBehind {
                val stroke = Stroke.nodeRing.toPx()
                val x = size.width * 0.35f

                drawLine(
                    color = colors.slate,
                    start = Offset(x, size.height * 0.2f),
                    end = Offset(size.width * 0.75f, size.height / 2),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.slate,
                    start = Offset(size.width * 0.75f, size.height / 2),
                    end = Offset(x, size.height * 0.8f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        )
    }
}
