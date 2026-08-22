package com.archieapps.calendar.feature.tasks

import com.archieapps.calendar.core.net.TaskSummaryDto
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.Recurrence
import com.archieapps.calendar.feature.calendar.togglable
import com.archieapps.calendar.feature.calendar.relativeDays
import com.archieapps.calendar.feature.calendar.shortDate
import java.time.LocalDate

enum class TaskFilter(val value: String, val label: String) {
    Pending("pending", "pendentes"),
    Today("today", "hoje"),
    Upcoming("upcoming", "próximas"),
    Overdue("overdue", "atrasadas"),
    All("all", "todas"),
    Done("done", "concluídas");

    val isHistory: Boolean get() = this == Done

    companion object {
        fun of(value: String?): TaskFilter =
            entries.firstOrNull { it.value == value } ?: Pending
    }
}

enum class TaskBucket(val value: String) {
    Overdue("overdue"),
    Today("today"),
    Upcoming("upcoming"),
    Settled("settled");

    companion object {
        fun of(value: String?): TaskBucket? = entries.firstOrNull { it.value == value }
    }
}

enum class UndoKind { Completion, Cancellation }

data class TaskUndo(
    val occurrenceId: String,
    val kind: UndoKind,
    val completed: Boolean,
    val label: String,
)

data class TaskListState(
    val filter: TaskFilter = TaskFilter.Pending,
    val draftQuery: String = "",
    val query: String = "",
    val categoryIds: Set<Int> = emptySet(),
    val priorities: Set<String> = emptySet(),
    val rows: List<CalendarEntry> = emptyList(),
    val page: Int = 1,
    val lastPage: Int = 1,
    val total: Int = 0,
    val truncated: Boolean = false,
    val summary: TaskSummaryDto? = null,
    val loading: Boolean = false,
    val appending: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val tailError: String? = null,
    val notice: String? = null,
    val noticeTick: Int = 0,
    val undo: TaskUndo? = null,
    val loadedOn: LocalDate? = null,
    val stale: Boolean = false,
    val resetTick: Int = 0,
    val filtersOpen: Boolean = false,
    val needsCode: Boolean = false,
    val unauthorized: Boolean = false,
) {
    val empty: Boolean
        get() = rows.isEmpty() && !loading && error == null

    val canLoadMore: Boolean
        get() = page < lastPage && !loading && !appending && tailError == null && rows.isNotEmpty()

    val activeFilterCount: Int
        get() = listOf(query.isNotBlank(), categoryIds.isNotEmpty(), priorities.isNotEmpty()).count { it }

    val categoriesParam: String?
        get() = categoryIds.sorted().joinToString(",").ifBlank { null }

    val prioritiesParam: String?
        get() = priorities.sorted().joinToString(",").ifBlank { null }

    fun countOf(filter: TaskFilter): Int? = summary?.counts?.get(filter.value)
}

fun List<CalendarEntry>.patchRowCompletion(occurrenceId: String, completed: Boolean): List<CalendarEntry> =
    map { row -> if (row.id == occurrenceId) row.copy(completed = completed) else row }

fun List<CalendarEntry>.withoutRow(occurrenceId: String): List<CalendarEntry> =
    filterNot { it.id == occurrenceId }

fun List<CalendarEntry>.withoutSeries(eventId: Int?): List<CalendarEntry> =
    if (eventId == null) this else filterNot { it.eventId == eventId }

fun mergeRows(current: List<CalendarEntry>, page: List<CalendarEntry>, fresh: Boolean): List<CalendarEntry> {
    if (fresh) {
        return page
    }

    val known = current.mapTo(mutableSetOf()) { it.id }

    return current + page.filterNot { known.contains(it.id) }
}

fun anchorCaption(entry: CalendarEntry, today: LocalDate = LocalDate.now()): String {
    val bucket = TaskBucket.of(entry.bucket)

    return when {
        bucket == TaskBucket.Overdue -> "venceu ${relativeDays(entry.dueDate ?: entry.date, today)}"

        bucket == TaskBucket.Settled && entry.completed ->
            "concluída ${relativeDays(entry.date, today)}"

        entry.date == today -> "hoje"

        bucket == TaskBucket.Upcoming && entry.recurring ->
            "próxima ${nearLabel(entry.date, today)}"

        else -> nearLabel(entry.date, today)
    }
}

fun dueCaption(entry: CalendarEntry, today: LocalDate = LocalDate.now()): String? {
    val due = entry.dueDate ?: return null

    if (!entry.hasOwnDueDate || TaskBucket.of(entry.bucket) == TaskBucket.Overdue) {
        return null
    }

    return "vence ${nearLabel(due, today)}"
}

fun recurrenceCaption(entry: CalendarEntry): String? {
    if (!entry.recurring) {
        return null
    }

    val rule = entry.recurrenceRule ?: return "repete"

    return Recurrence.parse(rule).compact(entry.date).takeIf { it != "não repete" } ?: "repete"
}

fun horizonCaption(entry: CalendarEntry, today: LocalDate = LocalDate.now()): String? {
    val ends = entry.recurrenceEndsAt ?: return null

    return if (entry.recurring) "até ${shortDate(ends, today)}" else null
}

fun streakCaption(entry: CalendarEntry): String? {
    val streak = entry.streak ?: return null

    return when {
        streak < 2 -> null
        else -> "$streak dias seguidos"
    }
}

fun rowLabel(entry: CalendarEntry, today: LocalDate = LocalDate.now()): String =
    buildList {
        add(if (entry.completed) "concluída" else "pendente")
        add(entry.title)
        add(anchorCaption(entry, today))
        entry.clock?.let { add(it) }
        recurrenceCaption(entry)?.let { add(it) }
        dueCaption(entry, today)?.let { add(it) }
        entry.categoryName?.let { add(it) }
    }.joinToString(", ")

private fun nearLabel(date: LocalDate, today: LocalDate): String {
    val span = java.time.temporal.ChronoUnit.DAYS.between(today, date)

    return if (span in -7..7) relativeDays(date, today) else shortDate(date, today)
}
