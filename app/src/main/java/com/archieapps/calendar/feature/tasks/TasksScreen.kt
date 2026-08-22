package com.archieapps.calendar.feature.tasks

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.core.net.TaskSummaryDto
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
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.SearchButton
import com.archieapps.calendar.design.components.TextAction
import com.archieapps.calendar.feature.calendar.CalendarEntry
import java.time.LocalDate

private const val prefetchRows = 4

@Composable
fun TasksScreen(
    state: TaskListState,
    onFilter: (TaskFilter) -> Unit,
    onQuery: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenFilters: () -> Unit,
    onQuickAdd: (String) -> Unit,
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
    val today = LocalDate.now()

    var searching by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

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
        Column(Modifier.padding(horizontal = Space.lg)) {
            Spacer(Modifier.height(Space.xl))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("tarefas", style = MonthTitle, color = colors.ink)

                Spacer(Modifier.weight(1f))

                if (state.activeFilterCount > 0) {
                    TextAction("limpar", onClearFilters, colors.brand, style = Eyebrow, horizontal = Space.sm)
                }

                SearchButton(
                    label = if (searching) "Fechar busca" else "Buscar tarefa",
                    onClick = {
                        searching = !searching

                        if (!searching && state.query.isNotBlank()) onQuery("")
                    },
                )

                Spacer(Modifier.width(Space.sm))

                CircleButton(
                    glyph = if (state.activeFilterCount > 0) "•" else "≡",
                    label = "Filtros",
                    onClick = onOpenFilters,
                    diameter = 36,
                )
            }

            state.summary?.let { summary ->
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = summaryLine(summary),
                    style = Eyebrow,
                    color = colors.slate,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Spacer(Modifier.height(Space.lg))

            ScrollingPills {
                TaskFilter.entries.forEach { filter ->
                    Pill(
                        label = pillLabel(filter, state.countOf(filter)),
                        selected = state.filter == filter,
                        onClick = { onFilter(filter) },
                    )
                }
            }

            Spacer(Modifier.height(Space.lg))

            if (searching) {
                HairlineField(
                    value = state.draftQuery,
                    onValueChange = onQuery,
                    label = "buscar",
                    placeholder = "título da tarefa",
                    imeAction = ImeAction.Search,
                    onImeAction = onSubmitQuery,
                )
            } else {
                HairlineField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = "nova tarefa",
                    placeholder = "o que precisa ser feito hoje",
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        onQuickAdd(draft)
                        draft = ""
                        focus.clearFocus()
                    },
                )
            }

            Spacer(Modifier.height(Space.lg))
        }

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
            TasksEmpty(
                filter = state.filter,
                filtered = state.activeFilterCount > 0,
                onClearFilters = onClearFilters,
            )
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
            items(state.rows, key = { it.id }) { entry ->
                TaskRowItem(
                    entry = entry,
                    today = today,
                    onOpen = { onOpen(entry) },
                    onToggle = { onToggle(entry) },
                )
            }

            item(key = "tail", contentType = "tail") {
                TasksTail(state = state, onRetryTail = onRetryTail, onOpenFilters = onOpenFilters)
            }
        }
    }
}

@Composable
private fun TasksEmpty(filter: TaskFilter, filtered: Boolean, onClearFilters: () -> Unit) {
    val colors = LocalChronicle.current

    Column(Modifier.padding(horizontal = Space.lg, vertical = Space.lg)) {
        Text(
            text = if (filtered) "Nada encontrado com esses filtros." else emptyLine(filter),
            style = EntryTitle,
            color = colors.slate,
        )

        if (filtered) {
            TextAction("limpar filtros", onClearFilters, colors.brand, style = Eyebrow)
        }
    }
}

@Composable
private fun TasksTail(state: TaskListState, onRetryTail: () -> Unit, onOpenFilters: () -> Unit) {
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

            state.truncated -> {
                Text(
                    text = "há mais tarefas do que cabe numa varredura",
                    style = EntryMeta,
                    color = colors.slate,
                )
                TextAction("estreitar por filtro", onOpenFilters, colors.brand, style = Eyebrow)
            }

            state.rows.isNotEmpty() && state.page >= state.lastPage -> Text(
                text = tailLine(state).uppercase(),
                style = Eyebrow,
                color = colors.slate,
            )
        }
    }
}

private fun pillLabel(filter: TaskFilter, count: Int?): String =
    if (count == null || count == 0) filter.label else "${filter.label} · $count"

private fun summaryLine(summary: TaskSummaryDto): String =
    buildList {
        if (summary.day.scheduled > 0) {
            add("${summary.day.completed} de ${summary.day.scheduled} hoje")
        }
        if (summary.streak.days > 1) {
            add("${summary.streak.days} dias seguidos")
        }
        summary.rate.percent?.let { add("$it% em ${summary.rate.days} dias") }
    }.joinToString(" · ").ifBlank { "nenhuma tarefa para hoje" }

private fun emptyLine(filter: TaskFilter): String = when (filter) {
    TaskFilter.Pending -> "Nada pendente. Dia limpo."
    TaskFilter.Today -> "Nada para hoje."
    TaskFilter.Upcoming -> "Nada marcado daqui pra frente."
    TaskFilter.Overdue -> "Nenhuma tarefa atrasada."
    TaskFilter.All -> "Nenhuma tarefa cadastrada."
    TaskFilter.Done -> "Nenhuma tarefa concluída ainda."
}

private fun tailLine(state: TaskListState): String =
    if (state.total == 1) "1 tarefa" else "${state.total} tarefas"
