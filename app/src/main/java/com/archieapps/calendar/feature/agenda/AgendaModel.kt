package com.archieapps.calendar.feature.agenda

import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.relativeDays
import com.archieapps.calendar.feature.calendar.shortDate
import java.time.LocalDate

data class AgendaDay(
    val date: LocalDate,
    val label: String,
    val relative: String?,
    val entries: List<CalendarEntry>,
)

data class AgendaFilters(
    val from: LocalDate = LocalDate.now(),
    val query: String = "",
    val categoryIds: Set<Int> = emptySet(),
    val kinds: Set<String> = agendaKinds.map { it.first }.toSet(),
    val natures: Set<String> = agendaNatures.map { it.first }.toSet(),
) {
    val queryParam: String?
        get() = query.trim().ifBlank { null }

    val categoriesParam: String?
        get() = categoryIds.sorted().joinToString(",").ifBlank { null }

    val kindsParam: String?
        get() = if (kinds == defaultKinds) null else kinds.sorted().joinToString(",")

    val naturesParam: String?
        get() = if (natures == defaultNatures) null else natures.sorted().joinToString(",")

    val activeCount: Int
        get() = listOf(
            queryParam != null,
            categoryIds.isNotEmpty(),
            kinds != defaultKinds,
            natures != defaultNatures,
        ).count { it }

    val anchoredToday: Boolean
        get() = from == LocalDate.now()
}

val agendaKinds: List<Pair<String, String>> = listOf(
    "event" to "eventos",
    "birthday" to "aniversários",
    "episode" to "episódios",
)

val agendaNatures: List<Pair<String, String>> = listOf(
    "event" to "compromisso",
    "task" to "tarefa",
)

private val defaultKinds: Set<String> = agendaKinds.map { it.first }.toSet()

private val defaultNatures: Set<String> = agendaNatures.map { it.first }.toSet()

private const val relativeWindowDays = 7L

fun dayCaption(date: LocalDate, today: LocalDate): Pair<String, String?> {
    val span = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    val relative = if (span in -relativeWindowDays..relativeWindowDays) relativeDays(date, today) else null

    return shortDate(date, today) to relative
}

fun groupEntries(entries: List<CalendarEntry>, today: LocalDate): List<AgendaDay> =
    entries.groupBy { it.date }.map { (date, items) ->
        val (label, relative) = dayCaption(date, today)

        AgendaDay(date = date, label = label, relative = relative, entries = items)
    }

fun mergeDays(current: List<AgendaDay>, page: List<CalendarEntry>, today: LocalDate): List<AgendaDay> {
    if (page.isEmpty()) {
        return current
    }

    val grouped = groupEntries(page, today)
    val tail = current.lastOrNull()

    if (tail == null) {
        return grouped
    }

    val first = grouped.first()

    if (first.date != tail.date) {
        return current + grouped
    }

    val joined = tail.copy(entries = tail.entries + first.entries)

    return current.dropLast(1) + joined + grouped.drop(1)
}

fun List<AgendaDay>.patchCompletion(occurrenceId: String, completed: Boolean): List<AgendaDay> =
    map { day ->
        if (day.entries.none { it.id == occurrenceId }) {
            day
        } else {
            day.copy(
                entries = day.entries.map { entry ->
                    if (entry.id == occurrenceId) entry.copy(completed = completed) else entry
                },
            )
        }
    }

fun List<AgendaDay>.withoutOccurrence(occurrenceId: String): List<AgendaDay> =
    mapNotNull { day ->
        if (day.entries.none { it.id == occurrenceId }) {
            day
        } else {
            val kept = day.entries.filterNot { it.id == occurrenceId }

            if (kept.isEmpty()) null else day.copy(entries = kept)
        }
    }

fun List<AgendaDay>.withoutEvent(eventId: Int): List<AgendaDay> =
    mapNotNull { day ->
        if (day.entries.none { it.eventId == eventId }) {
            day
        } else {
            val kept = day.entries.filterNot { it.eventId == eventId }

            if (kept.isEmpty()) null else day.copy(entries = kept)
        }
    }

fun agendaLabel(entry: CalendarEntry, today: LocalDate): String {
    val (label, relative) = dayCaption(entry.date, today)

    return buildList {
        add(relative ?: label)
        entry.clock?.let { add(it) }
        if (entry.allDay) add("dia inteiro")
        add(entry.title)
        entry.categoryName?.let { add(it) }
        entry.note?.let { add(it) }
        if (entry.completed) add("concluído")
    }.joinToString(", ")
}

data class AgendaState(
    val filters: AgendaFilters = AgendaFilters(),
    val draftQuery: String = "",
    val days: List<AgendaDay> = emptyList(),
    val seen: Set<String> = emptySet(),
    val cursor: String? = null,
    val pages: Int = 0,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val appending: Boolean = false,
    val error: String? = null,
    val tailError: String? = null,
    val notice: String? = null,
    val loadedOn: LocalDate? = null,
    val resetTick: Int = 0,
    val stale: Boolean = false,
    val filtersOpen: Boolean = false,
    val monthPickerOpen: Boolean = false,
    val unauthorized: Boolean = false,
) {
    val empty: Boolean
        get() = days.isEmpty() && !loading && error == null

    val canLoadMore: Boolean
        get() = hasMore && !loading && !appending && tailError == null && days.isNotEmpty()

    val total: Int
        get() = days.sumOf { it.entries.size }
}

const val agendaMaxPages: Int = 60

fun applyPage(
    state: AgendaState,
    entries: List<CalendarEntry>,
    hasMore: Boolean,
    nextCursor: String?,
    fresh: Boolean,
    today: LocalDate,
): AgendaState {
    val known = if (fresh) emptySet() else state.seen
    val incoming = entries.filterNot { known.contains(it.id) }
    val pages = state.pages + 1
    val exhausted = nextCursor == null ||
        pages >= agendaMaxPages ||
        (entries.isEmpty() && nextCursor == state.cursor)

    return state.copy(
        days = if (fresh) groupEntries(incoming, today) else mergeDays(state.days, incoming, today),
        seen = known + incoming.map { it.id },
        cursor = nextCursor,
        pages = pages,
        hasMore = hasMore && !exhausted,
        loading = false,
        appending = false,
        error = null,
        tailError = null,
        loadedOn = today,
        stale = false,
        resetTick = if (fresh) state.resetTick + 1 else state.resetTick,
    )
}
