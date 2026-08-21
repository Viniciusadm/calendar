package com.archieapps.calendar.feature.calendar

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarBoundsTest {
    @Test
    fun `range starts at january 2022`() {
        assertEquals(YearMonth.of(2022, 1), CalendarBounds.first)
    }

    @Test
    fun `range ends ten years ahead`() {
        assertEquals(YearMonth.now().plusYears(10), CalendarBounds.last)
    }

    @Test
    fun `clamp pulls months before the range up to the floor`() {
        assertEquals(CalendarBounds.first, CalendarBounds.clamp(YearMonth.of(2019, 7)))
        assertEquals(CalendarBounds.first, CalendarBounds.clamp(CalendarBounds.first.minusMonths(1)))
    }

    @Test
    fun `clamp pulls months after the range down to the ceiling`() {
        assertEquals(CalendarBounds.last, CalendarBounds.clamp(YearMonth.of(2050, 3)))
        assertEquals(CalendarBounds.last, CalendarBounds.clamp(CalendarBounds.last.plusMonths(1)))
    }

    @Test
    fun `clamp keeps months inside the range untouched`() {
        val inside = YearMonth.now()

        assertEquals(inside, CalendarBounds.clamp(inside))
    }

    @Test
    fun `has covers the edges and rejects what is outside`() {
        assertTrue(CalendarBounds.has(CalendarBounds.first))
        assertTrue(CalendarBounds.has(CalendarBounds.last))
        assertFalse(CalendarBounds.has(YearMonth.of(2021, 12)))
        assertFalse(CalendarBounds.has(CalendarBounds.last.plusMonths(1)))
    }

    @Test
    fun `first month sits on page zero and the last on the final page`() {
        assertEquals(0, CalendarBounds.pageOf(CalendarBounds.first))
        assertEquals(CalendarBounds.months - 1, CalendarBounds.pageOf(CalendarBounds.last))
    }

    @Test
    fun `pageOf and monthAt round trip across the whole range`() {
        (0 until CalendarBounds.months).forEach { page ->
            assertEquals(page, CalendarBounds.pageOf(CalendarBounds.monthAt(page)))
        }
    }

    @Test
    fun `years list spans both ends of the range`() {
        assertEquals(CalendarBounds.first.year, CalendarBounds.years.first())
        assertEquals(CalendarBounds.last.year, CalendarBounds.years.last())
    }
}
