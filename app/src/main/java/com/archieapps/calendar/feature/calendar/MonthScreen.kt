package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.components.CircleButton
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun MonthScreen(
    state: CalendarState,
    onSelect: (LocalDate) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
    onOpenEntry: (CalendarEntry) -> Unit,
    onToggleEntry: (CalendarEntry) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.xl))

        MonthHeader(
            state = state,
            onShiftMonth = onShiftMonth,
            onToday = onToday,
            onSignOut = onSignOut,
        )

        Spacer(Modifier.height(Space.xl))
        WeekdayStrip()
        Spacer(Modifier.height(Space.sm))
        MonthGrid(state = state, onSelect = onSelect)

        Spacer(Modifier.height(Space.lg))

        if (state.loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(Stroke.hairline),
                color = colors.brand,
                trackColor = colors.hairline,
            )
        } else {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(Stroke.hairline)
                    .drawBehind { drawLine(colors.hairline, Offset(0f, 0f), Offset(size.width, 0f), size.height) }
            )
        }

        state.error?.let { message ->
            Spacer(Modifier.height(Space.lg))
            Text(message, style = EntryMeta, color = colors.ink)
            TextButton(onClick = onRetry, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("Tentar de novo", style = Eyebrow, color = colors.brand)
            }
        }

        Spacer(Modifier.height(Space.xl))

        Text(
            text = dayHeadline(state.selected).uppercase(ptBr),
            style = Eyebrow,
            color = colors.slate,
        )

        Spacer(Modifier.height(Space.lg))

        DayAgenda(
            entries = state.selectedEntries,
            onOpen = onOpenEntry,
            onToggle = onToggleEntry,
        )

        Spacer(Modifier.height(Space.xxl))

        TextButton(onClick = onSignOut, contentPadding = PaddingValues(0.dp)) {
            Text("sair da conta", style = Eyebrow, color = colors.slate.copy(alpha = 0.75f))
        }

        Spacer(Modifier.height(Space.fabClearance))
    }
}

@Composable
private fun MonthHeader(
    state: CalendarState,
    onShiftMonth: (Long) -> Unit,
    onToday: () -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = LocalChronicle.current
    val monthName = state.month.month.getDisplayName(JavaTextStyle.FULL, ptBr)
    val isCurrentMonth = state.month == java.time.YearMonth.now()

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = monthName, style = MonthTitle, color = colors.ink)
            Spacer(Modifier.width(Space.sm))
            Text(
                text = state.month.year.toString(),
                style = MonthTitle.copy(fontSize = 22.sp, letterSpacing = (-0.6).sp),
                color = colors.slate.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(Space.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(glyph = "\u2039", label = "Mês anterior", onClick = { onShiftMonth(-1) })
            CircleButton(glyph = "\u203A", label = "Mês seguinte", onClick = { onShiftMonth(1) })

            Spacer(Modifier.weight(1f))

            if (!isCurrentMonth) {
                TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = Space.sm)) {
                    Text("hoje", style = Eyebrow, color = colors.brand)
                }
            }
        }
    }
}


private fun dayHeadline(date: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, ptBr)
    val month = date.month.getDisplayName(JavaTextStyle.FULL, ptBr)

    return "$weekday, ${date.dayOfMonth} de $month"
}
