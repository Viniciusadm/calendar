package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.components.Avatar
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import java.time.YearMonth
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun MonthScreen(
    state: CalendarState,
    onSelect: (LocalDate) -> Unit,
    onShowMonth: (YearMonth) -> Unit,
    onPickMonth: () -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
    onOpenEntry: (CalendarEntry) -> Unit,
    onToggleEntry: (CalendarEntry) -> Unit,
    onAccount: () -> Unit,
    accountInitial: String,
    accountPhoto: ImageBitmap?,
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
            onPickMonth = onPickMonth,
            onToday = onToday,
            onAccount = onAccount,
            accountInitial = accountInitial,
            accountPhoto = accountPhoto,
        )

        Spacer(Modifier.height(Space.xl))
        WeekdayStrip()
        Spacer(Modifier.height(Space.sm))

        MonthPager(state = state, onSelect = onSelect, onShowMonth = onShowMonth)

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

        Spacer(Modifier.height(Space.fabClearance))
    }
}

@Composable
private fun MonthHeader(
    state: CalendarState,
    onPickMonth: () -> Unit,
    onToday: () -> Unit,
    onAccount: () -> Unit,
    accountInitial: String,
    accountPhoto: ImageBitmap?,
) {
    val colors = LocalChronicle.current
    val monthName = state.month.month.getDisplayName(JavaTextStyle.FULL, ptBr)
    val isCurrentMonth = state.month == YearMonth.now()

    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HeaderMinHeight),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Space.sm))
                .clickable(onClick = onPickMonth)
                .semantics { contentDescription = "Escolher mês e ano" }
                .padding(end = Space.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = monthName, style = MonthTitle, color = colors.ink)
            Spacer(Modifier.width(Space.sm))
            Text(
                text = state.month.year.toString(),
                style = MonthTitle.copy(fontSize = 22.sp, letterSpacing = (-0.6).sp),
                color = colors.slate.copy(alpha = 0.7f),
            )
            Spacer(Modifier.width(Space.sm))
            Chevron(tint = colors.slate.copy(alpha = 0.7f))
        }

        Spacer(Modifier.weight(1f))

        if (!isCurrentMonth) {
            TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = Space.sm)) {
                Text("hoje", style = Eyebrow, color = colors.brand)
            }
        }

        Spacer(Modifier.width(Space.sm))

        Avatar(
            initial = accountInitial,
            label = "Sua conta",
            photo = accountPhoto,
            onClick = onAccount,
        )
    }
}

@Composable
private fun MonthPager(
    state: CalendarState,
    onSelect: (LocalDate) -> Unit,
    onShowMonth: (YearMonth) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = CalendarBounds.pageOf(state.month),
        pageCount = { CalendarBounds.months },
    )

    LaunchedEffect(pagerState.settledPage) {
        onShowMonth(CalendarBounds.monthAt(pagerState.settledPage))
    }

    LaunchedEffect(state.month) {
        val target = CalendarBounds.pageOf(state.month)

        if (target == pagerState.currentPage) return@LaunchedEffect

        if (abs(target - pagerState.currentPage) > NEARBY_PAGES) pagerState.scrollToPage(target)
        else pagerState.animateScrollToPage(target)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) { page ->
        MonthGrid(month = CalendarBounds.monthAt(page), state = state, onSelect = onSelect)
    }
}

@Composable
private fun Chevron(tint: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .padding(bottom = ChevronLift)
            .size(width = ChevronWidth, height = ChevronHeight)
            .drawBehind {
                drawLine(
                    color = tint,
                    start = Offset(0f, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = Stroke.nodeRing.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width / 2, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = Stroke.nodeRing.toPx(),
                    cap = StrokeCap.Round,
                )
            }
    )
}

private val HeaderMinHeight = 48.dp

private val ChevronWidth = 11.dp

private val ChevronHeight = 6.dp

private val ChevronLift = 8.dp

private const val NEARBY_PAGES = 3


private fun dayHeadline(date: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, ptBr)
    val month = date.month.getDisplayName(JavaTextStyle.FULL, ptBr)

    return "$weekday, ${date.dayOfMonth} de $month"
}
