package com.archieapps.calendar.feature.calendar

import androidx.compose.ui.graphics.Color
import com.archieapps.calendar.core.net.OccurrenceDto
import com.archieapps.calendar.design.colorFromToken
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.jsonPrimitive

enum class Agency { Mine, Arrives, Happened }

object CalendarBounds {
    val first: YearMonth = YearMonth.of(2022, 1)
    val last: YearMonth = YearMonth.now().plusYears(10)
    val years: List<Int> = (first.year..last.year).toList()
    val months: Int = first.until(last, ChronoUnit.MONTHS).toInt() + 1

    fun clamp(month: YearMonth): YearMonth = month.coerceIn(first, last)

    fun has(month: YearMonth): Boolean = month >= first && month <= last

    fun pageOf(month: YearMonth): Int = first.until(clamp(month), ChronoUnit.MONTHS).toInt()

    fun monthAt(page: Int): YearMonth = first.plusMonths(page.toLong())
}

data class CalendarEntry(
    val id: String,
    val eventId: Int?,
    val title: String,
    val date: LocalDate,
    val clock: String?,
    val allDay: Boolean,
    val color: Color,
    val agency: Agency,
    val kind: String,
    val nature: String,
    val completed: Boolean,
    val isTask: Boolean,
    val categoryName: String?,
    val note: String?,
    val recurring: Boolean,
    val editable: Boolean,
)

fun OccurrenceDto.toEntry(): CalendarEntry {
    val agency = when {
        nature == "milestone" -> Agency.Happened
        editable -> Agency.Mine
        else -> Agency.Arrives
    }

    return CalendarEntry(
        id = id,
        eventId = eventId,
        title = title,
        date = LocalDate.parse(date),
        clock = time,
        allDay = allDay,
        color = colorFromToken(color) ?: Color(0xFF349EF4),
        agency = agency,
        kind = kind,
        nature = nature,
        completed = completed,
        isTask = nature == "task",
        categoryName = categoryName,
        note = noteFromMeta(),
        recurring = recurring,
        editable = editable,
    )
}

private fun OccurrenceDto.noteFromMeta(): String? {
    val meta = meta ?: return null

    meta["age"]?.let { return "faz ${it.jsonPrimitive.content} anos" }

    return null
}
