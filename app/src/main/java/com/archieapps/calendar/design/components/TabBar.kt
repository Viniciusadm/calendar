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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke

enum class TabGlyph { List, Grid, Sliders }

data class TabItem(
    val label: String,
    val glyph: TabGlyph,
    val badge: Int? = null,
)

private val glyphBox = 22.dp
private val markWidth = 18.dp
private val barHeight = 58.dp

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
            .selectable(selected = active, onClick = onClick)
            .semantics { contentDescription = cellLabel(item, active) }
            .padding(vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Spacer(
                Modifier.size(glyphBox).drawBehind {
                    drawGlyph(item.glyph, tint, Stroke.nodeRing.toPx())
                }
            )

            if ((item.badge ?: 0) > 0) {
                Spacer(
                    Modifier.size(Stroke.node).drawBehind {
                        drawCircle(color = colors.brand, radius = size.minDimension / 2)
                    }
                )
            }
        }

        Spacer(Modifier.height(Space.xs))

        Text(item.label, style = Eyebrow, color = tint)

        Spacer(Modifier.height(Space.xxs))

        Spacer(
            Modifier.size(markWidth, Stroke.underline).drawBehind {
                if (active) {
                    drawLine(
                        color = colors.brand,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round,
                    )
                }
            }
        )
    }
}

private fun DrawScope.drawGlyph(glyph: TabGlyph, tint: Color, stroke: Float) {
    when (glyph) {
        TabGlyph.List -> {
            val rows = listOf(0.22f, 0.5f, 0.78f)
            val dotRadius = stroke * 1.1f

            rows.forEach { fraction ->
                val y = size.height * fraction

                drawCircle(color = tint, radius = dotRadius, center = Offset(size.width * 0.1f, y))
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.36f, y),
                    end = Offset(size.width * 0.94f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }

        TabGlyph.Grid -> {
            val top = size.height * 0.16f

            drawRect(
                color = tint,
                topLeft = Offset(size.width * 0.08f, top),
                size = Size(size.width * 0.84f, size.height * 0.76f),
                style = DrawStroke(width = stroke),
            )

            val headerY = top + size.height * 0.22f

            drawLine(
                color = tint,
                start = Offset(size.width * 0.08f, headerY),
                end = Offset(size.width * 0.92f, headerY),
                strokeWidth = stroke,
            )

            val dotRadius = stroke * 1.0f

            listOf(0.3f, 0.5f, 0.7f).forEach { fraction ->
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(size.width * fraction, size.height * 0.72f),
                )
            }
        }

        TabGlyph.Sliders -> {
            val rows = listOf(0.32f to 0.34f, 0.68f to 0.66f)

            rows.forEach { (rowFraction, knobFraction) ->
                val y = size.height * rowFraction

                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.08f, y),
                    end = Offset(size.width * 0.92f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )

                drawCircle(
                    color = tint,
                    radius = stroke * 1.9f,
                    center = Offset(size.width * knobFraction, y),
                    style = DrawStroke(width = stroke),
                )
            }
        }
    }
}

private fun cellLabel(item: TabItem, active: Boolean): String {
    val badge = item.badge ?: 0
    val suffix = if (badge > 0) ", $badge pendentes" else ""

    return if (active) "${item.label}, aba atual$suffix" else "${item.label}$suffix"
}
