package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryClock
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke

private val gutter = 28.dp
private val nodeCenterY = 13.dp

@Composable
fun DayAgenda(
    entries: List<CalendarEntry>,
    onOpen: (CalendarEntry) -> Unit,
    onToggle: (CalendarEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        EmptyDay(modifier)
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            SpineEntry(
                entry = entry,
                drawLineAbove = index > 0,
                drawLineBelow = index < entries.lastIndex,
                onOpen = { onOpen(entry) },
                onToggle = { onToggle(entry) },
            )
        }
    }
}

@Composable
private fun EmptyDay(modifier: Modifier = Modifier) {
    val colors = LocalChronicle.current

    Text(
        text = "Nada marcado.",
        style = EntryTitle,
        color = colors.ink,
        modifier = modifier.fillMaxWidth().padding(bottom = Space.lg),
    )
}

@Composable
private fun SpineEntry(
    entry: CalendarEntry,
    drawLineAbove: Boolean,
    drawLineBelow: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = LocalChronicle.current
    val dim = entry.completed

    val tappableNode = entry.isTask && entry.agency == Agency.Mine

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Spacer(
            Modifier
                .width(gutter)
                .then(
                    if (tappableNode) {
                        Modifier
                            .clickable(onClick = onToggle)
                            .semantics {
                                contentDescription = if (entry.completed) {
                                    "Reabrir ${entry.title}"
                                } else {
                                    "Concluir ${entry.title}"
                                }
                            }
                    } else {
                        Modifier
                    }
                )
                .fillMaxHeight()
                .drawBehind {
                    val x = gutter.toPx() / 2
                    val cy = nodeCenterY.toPx()
                    val hairline = Stroke.hairline.toPx()

                    if (drawLineAbove) {
                        drawLine(colors.hairline, Offset(x, 0f), Offset(x, cy), hairline)
                    }
                    if (drawLineBelow) {
                        drawLine(colors.hairline, Offset(x, cy), Offset(x, size.height), hairline)
                    }

                    val radius = Stroke.node.toPx() / 2

                    when (entry.agency) {
                        Agency.Mine -> drawCircle(
                            color = entry.color.copy(alpha = if (dim) 0.35f else 1f),
                            radius = radius,
                            center = Offset(x, cy),
                        )

                        Agency.Arrives -> drawCircle(
                            color = entry.color.copy(alpha = if (dim) 0.3f else 0.85f),
                            radius = radius,
                            center = Offset(x, cy),
                            style = DrawStroke(width = Stroke.nodeRing.toPx()),
                        )

                        Agency.Happened -> drawLine(
                            color = entry.color.copy(alpha = 0.75f),
                            start = Offset(x - radius - 2f, cy),
                            end = Offset(x + radius + 2f, cy),
                            strokeWidth = Stroke.nodeRing.toPx() * 1.4f,
                        )
                    }
                }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .semantics(mergeDescendants = true) { contentDescription = rowLabel(entry) }
                .padding(bottom = Space.lg)
        ) {
            Text(
                text = entry.title,
                style = EntryTitle,
                color = if (dim) colors.slate else colors.ink,
                textDecoration = if (dim) TextDecoration.LineThrough else null,
            )

            val meta = buildList {
                entry.clock?.let { add(it) }
                if (entry.allDay && entry.agency == Agency.Mine) add("dia inteiro")
                if (entry.agency == Agency.Happened) add("marco")
                entry.note?.let { add(it) }
                entry.categoryName?.let { add(it) }
            }

            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(Space.xxs))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(
                        text = meta.first(),
                        style = if (entry.clock != null) EntryClock else EntryMeta,
                        color = if (entry.clock != null) entry.color else colors.slate,
                    )
                    if (meta.size > 1) {
                        Text(
                            text = meta.drop(1).joinToString(" · "),
                            style = EntryMeta,
                            color = colors.slate,
                        )
                    }
                }
            }
        }
    }
}

private fun rowLabel(entry: CalendarEntry): String =
    buildList {
        add(entry.title)
        entry.clock?.let { add(it) }
        if (entry.allDay && entry.agency == Agency.Mine) add("dia inteiro")
        entry.categoryName?.let { add(it) }
        entry.note?.let { add(it) }
        if (entry.completed) add("concluído")
    }.joinToString(", ")
