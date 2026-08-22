package com.archieapps.calendar.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space

data class TabItem(
    val label: String,
    val icon: ImageVector,
    val badge: Int? = null,
)

private val barHeight = 56.dp

private val dot = 6.dp

@Composable
fun TabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(modifier = modifier.fillMaxWidth().background(colors.ground)) {
        Hairline()

        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(barHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                TabCell(
                    item = item,
                    active = index == selected,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabCell(
    item: TabItem,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val tint = if (active) colors.brand else colors.slate

    Column(
        modifier = modifier
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = cellLabel(item, active) }
            .padding(vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Glyph(icon = item.icon, tint = tint, size = 20.dp)

            if ((item.badge ?: 0) > 0) {
                Spacer(
                    Modifier
                        .size(dot)
                        .offset(x = Space.md, y = -Space.xxs)
                        .clip(CircleShape)
                        .drawBehind { drawCircle(color = colors.brand) }
                )
            }
        }

        Spacer(Modifier.height(Space.xs))

        Text(item.label, style = Eyebrow, color = tint)
    }
}

private fun cellLabel(item: TabItem, active: Boolean): String {
    val badge = item.badge ?: 0
    val suffix = if (badge > 0) ", $badge pendentes" else ""

    return if (active) "${item.label}, aba atual$suffix" else "${item.label}$suffix"
}
