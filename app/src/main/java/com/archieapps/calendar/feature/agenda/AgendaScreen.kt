package com.archieapps.calendar.feature.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.components.CircleButton
import com.archieapps.calendar.design.components.Hairline
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.PillRow
import com.archieapps.calendar.design.components.TextAction
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.DayAgenda
import com.archieapps.calendar.feature.calendar.shortDate
import java.time.LocalDate

private const val prefetchRows = 4

@Composable
fun AgendaScreen(
    state: AgendaState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onOpenFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onOpen: (CalendarEntry) -> Unit,
    onToggle: (CalendarEntry) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onRetryTail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1

            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - prefetchRows
        }.collect { near -> if (near) onLoadMore() }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) focus.clearFocus()
        }
    }

    LaunchedEffect(state.resetTick) {
        if (state.resetTick > 0) listState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxSize()) {
        AgendaHeader(
            query = state.draftQuery,
            fromLabel = fromLabel(state.filters.from),
            activeCount = state.filters.activeCount,
            onBack = onBack,
            onQuery = onQuery,
            onSubmitQuery = onSubmitQuery,
            onOpenFilters = onOpenFilters,
            onClearFilters = onClearFilters,
        )

        if (state.loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(Stroke.hairline),
                color = colors.brand,
                trackColor = colors.hairline,
            )
        } else {
            Hairline()
        }

        state.error?.let { message ->
            Column(Modifier.padding(horizontal = Space.lg)) {
                Spacer(Modifier.height(Space.lg))
                Text(message, style = EntryMeta, color = colors.ink)
                TextAction("Tentar de novo", onRetry, colors.brand, style = Eyebrow)
            }
        }

        if (state.empty) {
            AgendaEmpty(filtered = state.filters.activeCount > 0, onClearFilters = onClearFilters)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Space.lg,
                end = Space.lg,
                top = Space.lg,
                bottom = Space.fabClearance,
            ),
        ) {
            state.days.forEach { day ->
                item(key = "day:${day.date}", contentType = "day") {
                    DayHeader(label = day.label, relative = day.relative)
                }

                item(key = "entries:${day.date}", contentType = "entries") {
                    DayAgenda(entries = day.entries, onOpen = onOpen, onToggle = onToggle)
                }
            }

            item(key = "tail", contentType = "tail") {
                AgendaTail(state = state, onRetryTail = onRetryTail)
            }
        }
    }
}

@Composable
private fun AgendaHeader(
    query: String,
    fromLabel: String,
    activeCount: Int,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onOpenFilters: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val colors = LocalChronicle.current

    Column(Modifier.padding(horizontal = Space.lg)) {
        Spacer(Modifier.height(Space.xl))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(glyph = "‹", label = "Voltar", onClick = onBack)

            Spacer(Modifier.width(Space.md))

            Text("agenda", style = MonthTitle.copy(fontSize = 26.sp), color = colors.ink)

            Spacer(Modifier.weight(1f))

            if (activeCount > 0) {
                TextAction("limpar", onClearFilters, colors.brand, style = Eyebrow, horizontal = Space.sm)
            }
        }

        Spacer(Modifier.height(Space.lg))

        HairlineField(
            value = query,
            onValueChange = onQuery,
            label = "buscar",
            placeholder = "título do evento",
            imeAction = ImeAction.Search,
            onImeAction = onSubmitQuery,
        )

        Spacer(Modifier.height(Space.lg))

        PillRow {
            Pill(label = fromLabel, selected = false, onClick = onOpenFilters)
            Pill(
                label = if (activeCount > 0) "filtros · $activeCount" else "filtros",
                selected = activeCount > 0,
                onClick = onOpenFilters,
            )
        }

        Spacer(Modifier.height(Space.lg))
    }
}

@Composable
private fun DayHeader(label: String, relative: String?) {
    val colors = LocalChronicle.current
    val caption = if (relative == null) label else "$label · $relative"

    Text(
        text = caption.uppercase(),
        style = Eyebrow,
        color = colors.slate,
        modifier = Modifier
            .padding(top = Space.md, bottom = Space.md)
            .semantics { heading() },
    )
}

@Composable
private fun AgendaTail(state: AgendaState, onRetryTail: () -> Unit) {
    val colors = LocalChronicle.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            state.tailError != null -> {
                Text(state.tailError, style = EntryMeta, color = colors.ink)
                TextAction("Tentar de novo", onRetryTail, colors.brand, style = Eyebrow)
            }

            state.appending -> Text(
                text = "carregando mais…",
                style = EntryMeta,
                color = colors.slate,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            state.days.isNotEmpty() && !state.hasMore -> Text(
                text = "fim da agenda".uppercase(),
                style = Eyebrow,
                color = colors.slate,
            )
        }
    }
}

@Composable
private fun AgendaEmpty(filtered: Boolean, onClearFilters: () -> Unit) {
    val colors = LocalChronicle.current

    Column(Modifier.padding(horizontal = Space.lg, vertical = Space.lg)) {
        Text(
            text = if (filtered) "Nada encontrado com esses filtros." else "Nada marcado daqui pra frente.",
            style = EntryTitle,
            color = colors.slate,
        )

        if (filtered) {
            TextAction("limpar filtros", onClearFilters, colors.brand, style = Eyebrow)
        }
    }
}

private fun fromLabel(from: LocalDate): String {
    val today = LocalDate.now()

    return if (from == today) "a partir de hoje" else "a partir de ${shortDate(from, today)}"
}
