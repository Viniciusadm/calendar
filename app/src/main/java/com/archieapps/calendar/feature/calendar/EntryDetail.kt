package com.archieapps.calendar.feature.calendar

import com.archieapps.calendar.core.net.EventDto
import java.time.LocalDate

sealed interface DetailState {
    data object Absent : DetailState

    data object Loading : DetailState

    data class Ready(val detail: EntryDetail) : DetailState
}

data class NextOccurrence(val date: LocalDate, val time: String?, val allDay: Boolean) {
    val label: String get() = shortDate(date)

    val relative: String get() = relativeDays(date)
}

data class ChecklistItem(val title: String, val durationMinutes: Int?)

data class EntryDetail(
    val next: List<NextOccurrence> = emptyList(),
    val repeatsUntil: LocalDate? = null,
    val reminders: List<String> = emptyList(),
    val items: List<ChecklistItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() = next.isEmpty() && repeatsUntil == null && reminders.isEmpty() && items.isEmpty()
}

fun EventDto.toDetail(): EntryDetail = EntryDetail(
    next = nextOccurrences.mapNotNull { entry ->
        parseDate(entry.date)?.let { NextOccurrence(it, entry.time, entry.allDay) }
    },
    repeatsUntil = parseDate(recurrenceEndsAt),
    reminders = reminders.filter { it.active }.map { reminderLabel(it.minutesBefore) },
    items = items.sortedBy { it.position }.map { ChecklistItem(it.title, it.durationMinutes) },
)

fun reminderLabel(minutes: Int): String {
    reminderChoices.firstOrNull { it.second == minutes }?.let { return it.first }

    if (minutes < MINUTES_IN_HOUR) return "$minutes min antes"

    if (minutes % MINUTES_IN_DAY == 0) {
        val days = minutes / MINUTES_IN_DAY

        return if (days == 1) "1 dia antes" else "$days dias antes"
    }

    if (minutes % MINUTES_IN_HOUR == 0) return "${minutes / MINUTES_IN_HOUR} h antes"

    return "$minutes min antes"
}

private const val MINUTES_IN_HOUR = 60

private const val MINUTES_IN_DAY = 1440

private fun parseDate(raw: String?): LocalDate? {
    val text = raw?.take(10)?.takeIf { it.length == 10 } ?: return null

    return runCatching { LocalDate.parse(text) }.getOrNull()
}
