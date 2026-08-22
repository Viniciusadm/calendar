package com.archieapps.calendar.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.TokenWarning
import com.archieapps.calendar.feature.calendar.CalendarEntry
import java.time.LocalDate

private val nodeBox = 34.dp
private val nodeDiameter = 20.dp

@Composable
fun TaskRowItem(
    entry: CalendarEntry,
    today: LocalDate,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val overdue = TaskBucket.of(entry.bucket) == TaskBucket.Overdue

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowLabel(entry, today) },
        verticalAlignment = Alignment.Top,
    ) {
        CheckNode(
            color = entry.color,
            completed = entry.completed,
            label = if (entry.completed) "Reabrir ${entry.title}" else "Concluir ${entry.title}",
            onClick = onToggle,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(top = Space.sm, bottom = Space.lg),
        ) {
            Text(
                text = entry.title,
                style = EntryTitle,
                color = if (entry.completed) colors.slate else colors.ink,
                textDecoration = if (entry.completed) TextDecoration.LineThrough else null,
            )

            Spacer(Modifier.height(Space.xxs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = anchorCaption(entry, today),
                    style = EntryMeta,
                    color = if (overdue) colors.destructive else colors.slate,
                )

                val trail = buildList {
                    entry.clock?.let { add(it) }
                    recurrenceCaption(entry)?.let { add("↻ $it") }
                    dueCaption(entry, today)?.let { add(it) }
                    horizonCaption(entry, today)?.let { add(it) }
                    entry.categoryName?.let { add(it) }
                    streakCaption(entry)?.let { add(it) }
                }

                if (trail.isNotEmpty()) {
                    Text(
                        text = " · " + trail.joinToString(" · "),
                        style = EntryMeta,
                        color = colors.slate,
                    )
                }
            }
        }

        PriorityMark(priority = entry.priority)
    }
}

@Composable
private fun CheckNode(
    color: Color,
    completed: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = Modifier
            .size(nodeBox)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier.size(nodeDiameter).drawBehind {
                val ring = Stroke.nodeRing.toPx()
                val radius = size.minDimension / 2 - ring / 2
                val center = Offset(size.width / 2, size.height / 2)

                if (completed) {
                    drawCircle(color = color.copy(alpha = 0.35f), radius = radius, center = center)

                    val arm = radius * 0.52f

                    drawLine(
                        color = colors.ground,
                        start = Offset(center.x - arm, center.y),
                        end = Offset(center.x - arm * 0.15f, center.y + arm * 0.72f),
                        strokeWidth = ring * 1.3f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.ground,
                        start = Offset(center.x - arm * 0.15f, center.y + arm * 0.72f),
                        end = Offset(center.x + arm, center.y - arm * 0.6f),
                        strokeWidth = ring * 1.3f,
                        cap = StrokeCap.Round,
                    )
                } else {
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = center,
                        style = DrawStroke(width = ring),
                    )
                }
            }
        )
    }
}

@Composable
private fun PriorityMark(priority: String) {
    val colors = LocalChronicle.current

    val tint = when (priority) {
        "high" -> colors.destructive
        "medium" -> TokenWarning
        else -> null
    }

    Box(
        modifier = Modifier
            .padding(start = Space.md)
            .height(nodeBox)
            .width(Stroke.node),
        contentAlignment = Alignment.Center,
    ) {
        if (tint != null) {
            Spacer(Modifier.size(Stroke.node).clip(CircleShape).background(tint))
        }
    }
}

