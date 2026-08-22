package com.archieapps.calendar.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.archieapps.calendar.core.net.TaskSummaryDto
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.components.CircleButton
import com.archieapps.calendar.design.components.Hairline
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.TextAction
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.composables.icons.lucide.ListFilter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import java.time.LocalDate

private const val prefetchRows = 4

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    state: TaskListState,
    onFilter: (TaskFilter) -> Unit,
    onQuery: (String) -> Unit,
    onSubmitQuery: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenFilters: () -> Unit,
    onQuickAdd: (String) -> Unit,
    onDetailedAdd: () -> Unit,
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
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1

            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - prefetchRows
        }.collect { near -> if (near) onLoadMore() }
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

                CircleButton(
                    icon = if (searching) Lucide.X else Lucide.Search,
                    label = if (searching) "Fechar busca" else "Buscar tarefa",
                    onClick = {
                        searching = !searching

                        if (!searching && state.query.isNotBlank()) onQuery("")
                    },
                    diameter = 38,
                )

                Spacer(Modifier.width(Space.sm))

                CircleButton(
                    icon = Lucide.ListFilter,
                    label = if (state.activeFilterCount > 0) "Filtros ativos" else "Filtros",
                    onClick = onOpenFilters,
                    diameter = 38,
                    tint = if (state.activeFilterCount > 0) colors.brand else null,
                )
            }

            Spacer(Modifier.height(Space.xs))

            Text(
                text = summaryLine(state.summary),
                style = EntryMeta,
                color = colors.slate,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            if (searching) {
                Spacer(Modifier.height(Space.lg))

                HairlineField(
                    value = state.draftQuery,
                    onValueChange = onQuery,
                    label = "",
                    placeholder = "título da tarefa",
                    textStyle = EntryTitle,
                    imeAction = ImeAction.Search,
                    onImeAction = onSubmitQuery,
                )
            }

            Spacer(Modifier.height(Space.lg))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                TaskFilter.entries.forEach { filter ->
                    Pill(
                        label = pillLabel(filter, state.countOf(filter)),
                        selected = state.filter == filter,
                        onClick = { onFilter(filter) },
                    )
                }
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

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Space.lg,
                end = Space.lg,
                top = Space.md,
                bottom = Space.xxl,
            ),
        ) {
            item(key = "quickAdd", contentType = "quickAdd") {
                QuickAdd(
                    draft = draft,
                    expanded = adding,
                    onExpand = { adding = true },
                    onDraft = { draft = it },
                    onSubmit = {
                        if (draft.isNotBlank()) {
                            onQuickAdd(draft)
                            draft = ""
                            adding = false
                            focus.clearFocus()
                        }
                    },
                    onDetailed = {
                        adding = false
                        focus.clearFocus()
                        onDetailedAdd()
                    },
                )
            }

            state.error?.let { message ->
                item(key = "error", contentType = "error") {
                    Column(Modifier.padding(top = Space.lg)) {
                        Text(message, style = EntryMeta, color = colors.ink)
                        TextAction("Tentar de novo", onRetry, colors.brand, style = Eyebrow)
                    }
                }
            }

            if (state.empty && !adding) {
                item(key = "empty", contentType = "empty") {
                    TasksEmpty(
                        filter = state.filter,
                        filtered = state.activeFilterCount > 0,
                        onClearFilters = onClearFilters,
                    )
                }
            }

            items(state.rows, key = { it.id }, contentType = { "task" }) { entry ->
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

    Box(modifier = Modifier.fillMaxWidth().padding(top = Space.huge), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (filtered) "Nada com esses filtros." else emptyHeadline(filter),
                style = SheetTitle,
                color = colors.ink,
            )

            Spacer(Modifier.height(Space.sm))

            if (filtered) {
                TextAction("limpar filtros", onClearFilters, colors.brand, style = Eyebrow)
            } else {
                Text(emptyHint(filter), style = EntryMeta, color = colors.slate)
            }
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
                Text("há mais do que cabe numa varredura", style = EntryMeta, color = colors.slate)
                TextAction("estreitar por filtro", onOpenFilters, colors.brand, style = Eyebrow)
            }

            state.rows.size > 6 && state.page >= state.lastPage -> Text(
                text = tailLine(state).uppercase(),
                style = Eyebrow,
                color = colors.slate,
            )
        }
    }
}

private fun pillLabel(filter: TaskFilter, count: Int?): String =
    if (count == null || count == 0) filter.label else "${filter.label} $count"

private fun summaryLine(summary: TaskSummaryDto?): String {
    if (summary == null) {
        return "carregando…"
    }

    val parts = buildList {
        if (summary.day.scheduled > 0) {
            add("${summary.day.completed} de ${summary.day.scheduled} hoje")
        }
        if (summary.streak.days > 1) {
            add("${summary.streak.days} dias seguidos")
        }
        summary.rate.percent?.let { add("$it% em ${summary.rate.days} dias") }
    }

    return parts.joinToString(" · ").ifBlank { "sem tarefa marcada para hoje" }
}

private fun emptyHeadline(filter: TaskFilter): String = when (filter) {
    TaskFilter.Today -> "Dia limpo."
    TaskFilter.Upcoming -> "Nada à frente."
    TaskFilter.Overdue -> "Nada atrasado."
    TaskFilter.Done -> "Nada concluído ainda."
}

private fun emptyHint(filter: TaskFilter): String = when (filter) {
    TaskFilter.Overdue -> "tarefa que repete não entra aqui"
    TaskFilter.Done -> "o que você marcar aparece aqui"
    TaskFilter.Upcoming -> "toque em nova tarefa para agendar"
    else -> "toque em nova tarefa para começar"
}

private fun tailLine(state: TaskListState): String =
    if (state.total == 1) "1 tarefa" else "${state.total} tarefas"
