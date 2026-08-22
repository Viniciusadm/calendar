package com.archieapps.calendar.feature.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrencePresetTest {
    private val today = LocalDate.of(2026, 8, 21)

    @Test
    fun `every example from the brief maps to a rule`() {
        assertEquals(null, RecurrencePreset.Today.ruleFor(today).rule(today))
        assertEquals(null, RecurrencePreset.Once.ruleFor(today).rule(today))
        assertEquals("FREQ=DAILY", RecurrencePreset.Daily.ruleFor(today).rule(today))
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
            RecurrencePreset.Weekdays.ruleFor(today).rule(today),
        )
        assertEquals("FREQ=WEEKLY;BYDAY=FR", RecurrencePreset.Weekly.ruleFor(today).rule(today))
        assertEquals("FREQ=MONTHLY", RecurrencePreset.Monthly.ruleFor(today).rule(today))
        assertEquals("FREQ=YEARLY", RecurrencePreset.Yearly.ruleFor(today).rule(today))
    }

    @Test
    fun `estudar segunda quarta e sexta`() {
        val rule = Recurrence(
            unit = RepeatUnit.Week,
            weekdays = setOf(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.FRIDAY,
            ),
        )

        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE,FR", rule.rule(today))
        assertEquals(RecurrencePreset.Custom, presetOf(rule, today, today))
    }

    @Test
    fun `academia de setembro a novembro keeps its preset`() {
        val start = LocalDate.of(2026, 9, 1)
        val rule = RecurrencePreset.Weekdays
            .ruleFor(start)
            .copy(ending = RepeatEnding.Until, until = LocalDate.of(2026, 11, 30))
            .normalized(start)

        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;UNTIL=20261130",
            rule.rule(start),
        )
        assertEquals(RecurrencePreset.Weekdays, presetOf(rule, start, today))
    }

    @Test
    fun `preset detection round-trips through the rule`() {
        RecurrencePreset.entries
            .filter { it != RecurrencePreset.Custom && it != RecurrencePreset.Once }
            .forEach { preset ->
                val rule = preset.ruleFor(today)

                assertEquals(preset, presetOf(rule, today, today))
            }
    }

    @Test
    fun `once and today differ only by the date`() {
        assertEquals(RecurrencePreset.Today, presetOf(Recurrence(), today, today))
        assertEquals(RecurrencePreset.Once, presetOf(Recurrence(), today.plusDays(2), today))
    }

    @Test
    fun `an interval or an ordinal month falls to custom`() {
        val everyOtherDay = Recurrence(unit = RepeatUnit.Day, interval = 2)
        val nthWeekday = Recurrence(unit = RepeatUnit.Month, monthlyMode = MonthlyMode.WeekdayOfMonth)

        assertEquals(RecurrencePreset.Custom, presetOf(everyOtherDay, today, today))
        assertEquals(RecurrencePreset.Custom, presetOf(nthWeekday, today, today))
    }

    @Test
    fun `switching preset keeps the ending the user chose`() {
        val bounded = RecurrencePreset.Daily
            .ruleFor(today)
            .copy(ending = RepeatEnding.Count, count = 5)

        val switched = RecurrencePreset.Monthly.ruleFor(today, bounded)

        assertEquals(RepeatEnding.Count, switched.ending)
        assertEquals(5, switched.count)
        assertEquals("FREQ=MONTHLY;COUNT=5", switched.rule(today))
    }
}
