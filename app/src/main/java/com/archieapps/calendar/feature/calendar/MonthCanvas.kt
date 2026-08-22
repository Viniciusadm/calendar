package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.DayNumeral
import com.archieapps.calendar.design.DayNumeralStrong
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.WeekdayLabel
import java.time.LocalDate
import java.time.YearMonth



private val DayDisc = 34.dp

@Composable
fun WeekdayStrip(modifier: Modifier = Modifier) {
    val colors = LocalChronicle.current

    Row(modifier = modifier.fillMaxWidth()) {
        WeekStart.initials().forEach { initial ->
            Text(
                text = initial,
                style = WeekdayLabel,
                color = colors.slate,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    state: CalendarState,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val monthEntries = state.entriesFor(month)

    Column(modifier = modifier.fillMaxWidth()) {
        month.gridDays().chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        inMonth = YearMonth.from(day) == month,
                        isToday = day == today,
                        isSelected = day == state.selected,
                        entries = monthEntries[day].orEmpty(),
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    entries: List<CalendarEntry>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    val numeralColor = when {
        isToday -> if (colors.isDark) Color(0xFF04131F) else Color.White
        !inMonth -> colors.slate.copy(alpha = 0.38f)
        else -> colors.ink
    }

    Column(
        modifier = modifier
            .clickable { onSelect(day) }
            .padding(vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.defaultMinSize(minWidth = DayDisc, minHeight = DayDisc),
            contentAlignment = Alignment.Center,
        ) {
            val disc = Modifier
                .size(DayDisc)
                .clip(CircleShape)

            when {
                isToday -> Spacer(disc.background(colors.brand))
                isSelected -> Spacer(
                    disc.drawBehind {
                        drawCircle(
                            color = colors.brand,
                            radius = size.minDimension / 2 - Stroke.nodeRing.toPx(),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = Stroke.nodeRing.toPx()),
                        )
                    }
                )
            }

            Text(
                text = day.dayOfMonth.toString(),
                style = if (isToday || isSelected) DayNumeralStrong else DayNumeral,
                color = numeralColor,
            )
        }

        Spacer(Modifier.height(Space.xs))
        AgencyUnderline(entries = entries, dimmed = !inMonth)
    }
}

@Composable
private fun AgencyUnderline(entries: List<CalendarEntry>, dimmed: Boolean) {
    val segments = entries.asSequence()
        .sortedBy { it.agency.ordinal }
        .distinctBy { it.color.value to it.agency }
        .take(3)
        .toList()

    val height = Stroke.underline
    val width = 20.dp

    if (segments.isEmpty()) {
        Spacer(Modifier.height(height).width(width))
        return
    }

    Spacer(
        Modifier
            .height(height)
            .width(width)
            .drawBehind {
                val gap = 2.dp.toPx()
                val total = size.width - gap * (segments.size - 1)
                val each = total / segments.size

                segments.forEachIndexed { index, entry ->
                    val alpha = when (entry.agency) {
                        Agency.Mine -> 1f
                        Agency.Arrives -> 0.5f
                        Agency.Happened -> 0.32f
                    } * if (dimmed) 0.4f else 1f

                    drawRoundRect(
                        color = entry.color.copy(alpha = alpha),
                        topLeft = Offset(index * (each + gap), 0f),
                        size = Size(each, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    )
                }
            }
    )
}
