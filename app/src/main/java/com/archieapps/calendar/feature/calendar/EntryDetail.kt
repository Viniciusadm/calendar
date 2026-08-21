package com.archieapps.calendar.feature.calendar

import com.archieapps.calendar.core.net.EventDto
import com.archieapps.calendar.core.net.ProjectionDetailDto
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

data class Progress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)

    val label: String get() = "$done de $total assistidos"
}

data class EntryDetail(
    val next: List<NextOccurrence> = emptyList(),
    val repeatsUntil: LocalDate? = null,
    val reminders: List<String> = emptyList(),
    val items: List<ChecklistItem> = emptyList(),
    val facts: List<String> = emptyList(),
    val note: String? = null,
    val contacts: List<String> = emptyList(),
    val progress: Progress? = null,
    val locked: Boolean = false,
) {
    val isEmpty: Boolean
        get() = next.isEmpty() && repeatsUntil == null && reminders.isEmpty() && items.isEmpty() &&
            facts.isEmpty() && note == null && contacts.isEmpty() && progress == null && !locked
}

fun EventDto.toDetail(): EntryDetail = EntryDetail(
    next = nextOccurrences.mapNotNull { entry ->
        parseDate(entry.date)?.let { NextOccurrence(it, entry.time, entry.allDay) }
    },
    repeatsUntil = parseDate(recurrenceEndsAt),
    reminders = reminders.filter { it.active }.map { reminderLabel(it.minutesBefore) },
    items = items.sortedBy { it.position }.map { ChecklistItem(it.title, it.durationMinutes) },
)

fun ProjectionDetailDto.toDetail(date: LocalDate, today: LocalDate = LocalDate.now()): EntryDetail =
    when (kind) {
        "birthday" -> birthdayDetail(date, today)
        "episode" -> episodeDetail()
        else -> EntryDetail()
    }

private fun ProjectionDetailDto.birthdayDetail(date: LocalDate, today: LocalDate): EntryDetail = EntryDetail(
    facts = buildList {
        name?.trim()?.takeIf { it.isNotEmpty() && it != nickname?.trim() }?.let { add(it) }
        parseDate(birthDate)?.let { add("nasceu em ${dayLabel(it)}") }
        add("${weekdayName(date.dayOfWeek)} · ${relativeDays(date, today)}")
    },
    note = note.cleaned(),
    contacts = buildList {
        instagram.cleaned()?.let { add("@$it") }
        phone.cleaned()?.let { add(it) }
    },
    locked = locked,
)

private fun ProjectionDetailDto.episodeDetail(): EntryDetail = EntryDetail(
    facts = listOfNotNull(
        episode?.let { if (total == null) "episódio $it" else "episódio $it de $total" },
        durationMinutes?.let { "$it min" },
        type.cleaned(),
    ).let { parts -> if (parts.isEmpty()) emptyList() else listOf(parts.joinToString(SEPARATOR)) },
    progress = if (watched != null && total != null && total > 0) Progress(watched, total) else null,
)

private const val SEPARATOR = " · "

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

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
