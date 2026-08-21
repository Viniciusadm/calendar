package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.components.Pill
import java.time.YearMonth

private val monthLabels =
    listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

@Composable
fun MonthPickerSheet(
    current: YearMonth,
    onPick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    var year by remember { mutableIntStateOf(current.year) }
    val yearsState = rememberLazyListState(
        initialFirstVisibleItemIndex = (CalendarBounds.years.indexOf(current.year) - 1).coerceAtLeast(0),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.lg),
    ) {
        Text("ir para".uppercase(), style = Eyebrow, color = colors.slate)

        Spacer(Modifier.height(Space.lg))

        LazyRow(
            state = yearsState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            items(CalendarBounds.years) { candidate ->
                Pill(
                    label = candidate.toString(),
                    selected = candidate == year,
                    onClick = { year = candidate },
                )
            }
        }

        Spacer(Modifier.height(Space.xl))

        monthLabels.chunked(3).forEachIndexed { rowIndex, labels ->
            if (rowIndex > 0) Spacer(Modifier.height(Space.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                labels.forEachIndexed { columnIndex, label ->
                    val month = YearMonth.of(year, rowIndex * 3 + columnIndex + 1)

                    MonthCell(
                        label = label,
                        month = month,
                        selected = month == current,
                        onPick = onPick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun MonthCell(
    label: String,
    month: YearMonth,
    selected: Boolean,
    onPick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val reachable = CalendarBounds.has(month)
    val isThisMonth = month == YearMonth.now()

    val border = when {
        !reachable -> colors.hairline.copy(alpha = 0.4f)
        selected -> colors.brand.copy(alpha = 0.55f)
        else -> colors.hairline
    }

    val ink = when {
        !reachable -> colors.slate.copy(alpha = 0.3f)
        selected || isThisMonth -> colors.brand
        else -> colors.ink
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = CellHeight)
            .clip(cellShape)
            .background(if (selected) colors.brandSoft else Color.Transparent)
            .border(width = Stroke.hairline, color = border, shape = cellShape)
            .then(if (reachable) Modifier.clickable { onPick(month) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = ButtonLabel, color = ink)
    }
}

private val cellShape = RoundedCornerShape(Space.md)

private val CellHeight = 48.dp
