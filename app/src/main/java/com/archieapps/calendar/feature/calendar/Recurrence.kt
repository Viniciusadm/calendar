package com.archieapps.calendar.feature.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class RepeatUnit(val freq: String, val one: String, val many: String, val every: String) {
    Day("DAILY", "dia", "dias", "todo dia"),
    Week("WEEKLY", "semana", "semanas", "toda semana"),
    Month("MONTHLY", "mês", "meses", "todo mês"),
    Year("YEARLY", "ano", "anos", "todo ano"),
}

enum class MonthlyMode { DayOfMonth, WeekdayOfMonth }

enum class RepeatEnding { Never, Count, Until }

val weekOrder: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)

private val weekdayCodes = mapOf(
    DayOfWeek.MONDAY to "MO",
    DayOfWeek.TUESDAY to "TU",
    DayOfWeek.WEDNESDAY to "WE",
    DayOfWeek.THURSDAY to "TH",
    DayOfWeek.FRIDAY to "FR",
    DayOfWeek.SATURDAY to "SA",
    DayOfWeek.SUNDAY to "SU",
)

private val shortNames = mapOf(
    DayOfWeek.MONDAY to "seg",
    DayOfWeek.TUESDAY to "ter",
    DayOfWeek.WEDNESDAY to "qua",
    DayOfWeek.THURSDAY to "qui",
    DayOfWeek.FRIDAY to "sex",
    DayOfWeek.SATURDAY to "sáb",
    DayOfWeek.SUNDAY to "dom",
)

private val longNames = mapOf(
    DayOfWeek.MONDAY to "segunda",
    DayOfWeek.TUESDAY to "terça",
    DayOfWeek.WEDNESDAY to "quarta",
    DayOfWeek.THURSDAY to "quinta",
    DayOfWeek.FRIDAY to "sexta",
    DayOfWeek.SATURDAY to "sábado",
    DayOfWeek.SUNDAY to "domingo",
)

private val monthShort =
    listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

fun weekdayShort(day: DayOfWeek): String = shortNames.getValue(day)

fun weekdayName(day: DayOfWeek): String = longNames.getValue(day)

fun ordinalOf(date: LocalDate): Int =
    if (date.dayOfMonth + 7 > date.lengthOfMonth()) -1 else (date.dayOfMonth - 1) / 7 + 1

fun ordinalLabel(position: Int): String = if (position == -1) "última" else "${position}ª"

fun dayLabel(date: LocalDate): String =
    "${date.dayOfMonth} de ${monthShort[date.monthValue - 1]} de ${date.year}"

fun shortDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val stem = "${weekdayShort(date.dayOfWeek)}, ${date.dayOfMonth} ${monthShort[date.monthValue - 1]}"

    return if (date.year == today.year) stem else "$stem de ${date.year}"
}

fun relativeDays(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, date)

    return when {
        days == 0L -> "hoje"
        days == 1L -> "amanhã"
        days == -1L -> "ontem"
        days > 1L -> "em $days dias"
        else -> "há ${-days} dias"
    }
}

data class Recurrence(
    val unit: RepeatUnit? = null,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthlyMode: MonthlyMode = MonthlyMode.DayOfMonth,
    val setPos: Int? = null,
    val ending: RepeatEnding = RepeatEnding.Never,
    val count: Int = 10,
    val until: LocalDate? = null,
) {
    val repeats: Boolean get() = unit != null

    fun normalized(start: LocalDate): Recurrence {
        val fixed = copy(
            interval = interval.coerceIn(1, MAX_INTERVAL),
            count = count.coerceIn(1, MAX_COUNT),
        )

        val dated = when (fixed.ending) {
            RepeatEnding.Until -> fixed.copy(until = (fixed.until ?: defaultUntil(start)).coerceAtLeast(start))
            else -> fixed
        }

        return when {
            dated.unit == RepeatUnit.Month && dated.monthlyMode == MonthlyMode.WeekdayOfMonth ->
                dated.copy(weekdays = setOf(start.dayOfWeek), setPos = ordinalOf(start))

            else -> dated.copy(setPos = null)
        }
    }

    fun rule(start: LocalDate): String? = normalized(start).emit()

    fun summary(start: LocalDate? = null): String {
        val unit = unit ?: return "não repete"

        val parts = mutableListOf(if (interval <= 1) unit.every else "a cada $interval ${unit.many}")

        when (unit) {
            RepeatUnit.Week -> if (weekdays.isNotEmpty()) parts += weekdayList()

            RepeatUnit.Month -> if (monthlyMode == MonthlyMode.WeekdayOfMonth) {
                monthlyPattern(start)?.let { parts += it }
            } else {
                start?.let { parts += "no dia ${it.dayOfMonth}" }
            }

            else -> Unit
        }

        when (ending) {
            RepeatEnding.Count -> parts += if (count == 1) "1 vez" else "$count vezes"
            RepeatEnding.Until -> until?.let { parts += "até ${dayLabel(it)}" }
            RepeatEnding.Never -> Unit
        }

        return parts.joinToString(", ")
    }

    fun monthlyPattern(start: LocalDate?): String? {
        val day = weekdays.firstOrNull() ?: start?.dayOfWeek ?: return null
        val position = setPos ?: start?.let { ordinalOf(it) } ?: return null

        return "na ${ordinalLabel(position)} ${weekdayName(day)}"
    }

    private fun emit(): String? {
        val unit = unit ?: return null

        val pieces = mutableListOf("FREQ=${unit.freq}")

        if (interval > 1) pieces += "INTERVAL=$interval"

        val byDay = when {
            unit == RepeatUnit.Week -> weekdays
            unit == RepeatUnit.Month && monthlyMode == MonthlyMode.WeekdayOfMonth -> weekdays
            else -> emptySet()
        }

        if (byDay.isNotEmpty()) {
            pieces += "BYDAY=" + weekOrder.filter { it in byDay }.joinToString(",") { weekdayCodes.getValue(it) }
        }

        if (unit == RepeatUnit.Month && monthlyMode == MonthlyMode.WeekdayOfMonth && setPos != null) {
            pieces += "BYSETPOS=$setPos"
        }

        when (ending) {
            RepeatEnding.Count -> pieces += "COUNT=$count"
            RepeatEnding.Until -> until?.let { pieces += "UNTIL=" + it.format(compactDate) }
            RepeatEnding.Never -> Unit
        }

        return pieces.joinToString(";")
    }

    private fun weekdayList(): String {
        val names = weekOrder.filter { it in weekdays }.map { weekdayShort(it) }

        return when (names.size) {
            1 -> names.first()
            else -> names.dropLast(1).joinToString(", ") + " e " + names.last()
        }
    }

    companion object {
        const val MAX_INTERVAL = 366

        const val MAX_COUNT = 730

        private val compactDate: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

        fun defaultUntil(start: LocalDate): LocalDate = start.plusMonths(3)

        fun parse(raw: String?): Recurrence {
            val text = raw?.trim()?.uppercase()?.removePrefix("RRULE:")?.takeIf { it.isNotBlank() }
                ?: return Recurrence()

            val parts = text.split(';').mapNotNull { chunk ->
                val pair = chunk.split('=', limit = 2)

                if (pair.size == 2) pair[0].trim() to pair[1].trim() else null
            }.toMap()

            val unit = RepeatUnit.entries.firstOrNull { it.freq == parts["FREQ"] } ?: return Recurrence()
            val days = parseWeekdays(parts["BYDAY"])
            val position = parts["BYSETPOS"]?.toIntOrNull() ?: parseOrdinalPrefix(parts["BYDAY"])
            val weekdayOfMonth = unit == RepeatUnit.Month && days.isNotEmpty()
            val until = parseUntil(parts["UNTIL"])
            val count = parts["COUNT"]?.toIntOrNull()

            return Recurrence(
                unit = unit,
                interval = (parts["INTERVAL"]?.toIntOrNull() ?: 1).coerceIn(1, MAX_INTERVAL),
                weekdays = days,
                monthlyMode = if (weekdayOfMonth) MonthlyMode.WeekdayOfMonth else MonthlyMode.DayOfMonth,
                setPos = if (weekdayOfMonth) position else null,
                ending = when {
                    count != null -> RepeatEnding.Count
                    until != null -> RepeatEnding.Until
                    else -> RepeatEnding.Never
                },
                count = count?.coerceIn(1, MAX_COUNT) ?: 10,
                until = until,
            )
        }

        private fun parseWeekdays(raw: String?): Set<DayOfWeek> {
            val chunks = raw?.split(',') ?: return emptySet()

            return chunks.mapNotNull { chunk ->
                val code = chunk.trim().takeLast(2)

                weekdayCodes.entries.firstOrNull { it.value == code }?.key
            }.toSet()
        }

        private fun parseOrdinalPrefix(raw: String?): Int? {
            val chunk = raw?.split(',')?.firstOrNull()?.trim() ?: return null

            return chunk.dropLast(2).takeIf { it.isNotBlank() }?.toIntOrNull()
        }

        private fun parseUntil(raw: String?): LocalDate? {
            val date = raw?.take(8)?.takeIf { it.length == 8 } ?: return null

            return runCatching { LocalDate.parse(date, compactDate) }.getOrNull()
        }
    }
}
