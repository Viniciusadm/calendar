package com.archieapps.calendar.feature.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceTest {
    private val start: LocalDate = LocalDate.of(2026, 8, 12)

    @Test
    fun `no rule means no repetition`() {
        assertNull(Recurrence().rule(start))
        assertEquals(Recurrence(), Recurrence.parse(null))
        assertEquals(Recurrence(), Recurrence.parse("  "))
        assertEquals(Recurrence(), Recurrence.parse("FREQ=HOURLY"))
    }

    @Test
    fun `interval of one is omitted`() {
        assertEquals("FREQ=DAILY", Recurrence(unit = RepeatUnit.Day).rule(start))
    }

    @Test
    fun `every three days`() {
        val rule = Recurrence(unit = RepeatUnit.Day, interval = 3).rule(start)

        assertEquals("FREQ=DAILY;INTERVAL=3", rule)
        assertEquals(rule, Recurrence.parse(rule).rule(start))
    }

    @Test
    fun `every two weeks on wednesday and friday for ten times`() {
        val recurrence = Recurrence(
            unit = RepeatUnit.Week,
            interval = 2,
            weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.WEDNESDAY),
            ending = RepeatEnding.Count,
            count = 10,
        )

        val rule = recurrence.rule(start)

        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=WE,FR;COUNT=10", rule)
        assertEquals(rule, Recurrence.parse(rule).rule(start))
        assertEquals("a cada 2 semanas, qua e sex, 10 vezes", recurrence.summary(start))
    }

    @Test
    fun `monthly on the second wednesday`() {
        val rule = Recurrence(unit = RepeatUnit.Month, monthlyMode = MonthlyMode.WeekdayOfMonth).rule(start)

        assertEquals("FREQ=MONTHLY;BYDAY=WE;BYSETPOS=2", rule)
        assertEquals(rule, Recurrence.parse(rule).rule(start))
    }

    @Test
    fun `monthly on the last weekday of the month`() {
        val late = LocalDate.of(2026, 8, 26)
        val rule = Recurrence(unit = RepeatUnit.Month, monthlyMode = MonthlyMode.WeekdayOfMonth).rule(late)

        assertEquals("FREQ=MONTHLY;BYDAY=WE;BYSETPOS=-1", rule)
        assertEquals("todo mês, na última quarta", Recurrence.parse(rule).summary())
    }

    @Test
    fun `monthly on the day of month carries no byday`() {
        assertEquals("FREQ=MONTHLY", Recurrence(unit = RepeatUnit.Month).rule(start))
    }

    @Test
    fun `until is written as a compact date`() {
        val recurrence = Recurrence(
            unit = RepeatUnit.Year,
            ending = RepeatEnding.Until,
            until = LocalDate.of(2026, 12, 31),
        )

        assertEquals("FREQ=YEARLY;UNTIL=20261231", recurrence.rule(start))
        assertEquals(LocalDate.of(2026, 12, 31), Recurrence.parse("FREQ=YEARLY;UNTIL=20261231T235959Z").until)
    }

    @Test
    fun `until never falls before the start date`() {
        val recurrence = Recurrence(
            unit = RepeatUnit.Day,
            ending = RepeatEnding.Until,
            until = start.minusDays(5),
        ).normalized(start)

        assertEquals(start, recurrence.until)
    }

    @Test
    fun `until defaults to three months ahead`() {
        val recurrence = Recurrence(unit = RepeatUnit.Day, ending = RepeatEnding.Until).normalized(start)

        assertEquals(start.plusMonths(3), recurrence.until)
    }

    @Test
    fun `interval and count are clamped to the backend limits`() {
        val recurrence = Recurrence(
            unit = RepeatUnit.Day,
            interval = 9000,
            ending = RepeatEnding.Count,
            count = 9000,
        ).normalized(start)

        assertEquals(Recurrence.MAX_INTERVAL, recurrence.interval)
        assertEquals(Recurrence.MAX_COUNT, recurrence.count)
    }

    @Test
    fun `legacy rules keep working`() {
        val recurrence = Recurrence.parse("RRULE:FREQ=WEEKLY")

        assertEquals(RepeatUnit.Week, recurrence.unit)
        assertEquals(1, recurrence.interval)
        assertEquals(RepeatEnding.Never, recurrence.ending)
        assertEquals("toda semana", recurrence.summary())
    }

    @Test
    fun `ordinal prefixed byday is understood`() {
        val recurrence = Recurrence.parse("FREQ=MONTHLY;BYDAY=2TU")

        assertEquals(MonthlyMode.WeekdayOfMonth, recurrence.monthlyMode)
        assertEquals(setOf(DayOfWeek.TUESDAY), recurrence.weekdays)
        assertEquals(2, recurrence.setPos)
    }
}
