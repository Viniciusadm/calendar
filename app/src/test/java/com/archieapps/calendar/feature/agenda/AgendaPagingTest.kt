package com.archieapps.calendar.feature.agenda

import androidx.compose.ui.graphics.Color
import com.archieapps.calendar.feature.calendar.Agency
import com.archieapps.calendar.feature.calendar.CalendarEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AgendaPagingTest {
    private val today = LocalDate.of(2026, 8, 21)

    private fun entry(id: String, date: LocalDate, title: String = "Item"): CalendarEntry =
        CalendarEntry(
            id = id,
            eventId = id.substringAfter(':').substringBefore(':').toIntOrNull(),
            title = title,
            date = date,
            clock = null,
            allDay = true,
            color = Color.Unspecified,
            agency = Agency.Mine,
            kind = "event",
            nature = "event",
            completed = false,
            isTask = false,
            categoryName = null,
            note = null,
            description = null,
            endTime = null,
            durationMinutes = 1440,
            priority = "none",
            completedOn = null,
            overridden = false,
            remindersMuted = false,
            recurring = false,
            recurrenceRule = null,
            recurrenceEndsAt = null,
            dueDate = null,
            overdue = false,
            bucket = null,
            streak = null,
            editable = true,
            action = null,
        )

    @Test
    fun `first page groups by date preserving server order`() {
        val state = applyPage(
            state = AgendaState(),
            entries = listOf(
                entry("event:1:2026-08-21", today),
                entry("event:2:2026-08-21", today),
                entry("event:3:2026-08-23", today.plusDays(2)),
            ),
            hasMore = true,
            nextCursor = "c1",
            fresh = true,
            today = today,
        )

        assertEquals(listOf(today, today.plusDays(2)), state.days.map { it.date })
        assertEquals(2, state.days.first().entries.size)
        assertEquals(3, state.total)
        assertEquals("c1", state.cursor)
        assertTrue(state.hasMore)
    }

    @Test
    fun `a page continuing the same date extends the last group instead of duplicating it`() {
        val first = applyPage(
            AgendaState(),
            listOf(entry("event:1:2026-08-21", today)),
            hasMore = true,
            nextCursor = "c1",
            fresh = true,
            today = today,
        )

        val second = applyPage(
            first,
            listOf(
                entry("event:2:2026-08-21", today),
                entry("event:3:2026-08-22", today.plusDays(1)),
            ),
            hasMore = true,
            nextCursor = "c2",
            fresh = false,
            today = today,
        )

        assertEquals(listOf(today, today.plusDays(1)), second.days.map { it.date })
        assertEquals(2, second.days.first().entries.size)
        assertEquals(3, second.total)
    }

    @Test
    fun `appending reuses earlier day objects`() {
        val first = applyPage(
            AgendaState(),
            listOf(entry("event:1:2026-08-21", today), entry("event:2:2026-08-25", today.plusDays(4))),
            hasMore = true,
            nextCursor = "c1",
            fresh = true,
            today = today,
        )

        val second = applyPage(
            first,
            listOf(entry("event:3:2026-08-30", today.plusDays(9))),
            hasMore = true,
            nextCursor = "c2",
            fresh = false,
            today = today,
        )

        assertSame(first.days.first(), second.days.first())
    }

    @Test
    fun `a repeated occurrence id is dropped`() {
        val first = applyPage(
            AgendaState(),
            listOf(entry("event:1:2026-08-21", today)),
            hasMore = true,
            nextCursor = "c1",
            fresh = true,
            today = today,
        )

        val second = applyPage(
            first,
            listOf(entry("event:1:2026-08-21", today), entry("event:2:2026-08-22", today.plusDays(1))),
            hasMore = true,
            nextCursor = "c2",
            fresh = false,
            today = today,
        )

        assertEquals(2, second.total)
        assertEquals(listOf("event:1:2026-08-21", "event:2:2026-08-22"), second.days.flatMap { d -> d.entries.map { it.id } })
    }

    @Test
    fun `a null cursor ends the list`() {
        val state = applyPage(
            AgendaState(),
            listOf(entry("event:1:2026-08-21", today)),
            hasMore = true,
            nextCursor = null,
            fresh = true,
            today = today,
        )

        assertFalse(state.hasMore)
        assertNull(state.cursor)
    }

    @Test
    fun `an empty page repeating the same cursor ends the list`() {
        val first = AgendaState(cursor = "c1", days = emptyList())

        val state = applyPage(first, emptyList(), hasMore = true, nextCursor = "c1", fresh = false, today = today)

        assertFalse(state.hasMore)
    }

    @Test
    fun `an empty page with a new cursor keeps paging`() {
        val first = AgendaState(cursor = "c1")

        val state = applyPage(first, emptyList(), hasMore = true, nextCursor = "c2", fresh = false, today = today)

        assertTrue(state.hasMore)
    }

    @Test
    fun `the page cap stops the list`() {
        val state = applyPage(
            AgendaState(pages = agendaMaxPages - 1),
            listOf(entry("event:1:2026-08-21", today)),
            hasMore = true,
            nextCursor = "c1",
            fresh = false,
            today = today,
        )

        assertFalse(state.hasMore)
    }

    @Test
    fun `a fresh page replaces the accumulated list and bumps the reset tick`() {
        val first = applyPage(
            AgendaState(),
            listOf(entry("event:1:2026-08-21", today), entry("event:2:2026-08-22", today.plusDays(1))),
            hasMore = true,
            nextCursor = "c1",
            fresh = true,
            today = today,
        )

        val fresh = applyPage(
            first,
            listOf(entry("event:9:2026-09-01", today.plusDays(11))),
            hasMore = false,
            nextCursor = null,
            fresh = true,
            today = today,
        )

        assertEquals(1, fresh.total)
        assertEquals(listOf(today.plusDays(11)), fresh.days.map { it.date })
        assertEquals(setOf("event:9:2026-09-01"), fresh.seen)
        assertEquals(first.resetTick + 1, fresh.resetTick)
    }

    @Test
    fun `an id seen before a fresh reload is not filtered out`() {
        val first = applyPage(
            AgendaState(),
            listOf(entry("event:1:2026-08-21", today)),
            hasMore = true,
            nextCursor = "c1",
            fresh = true,
            today = today,
        )

        val again = applyPage(
            first,
            listOf(entry("event:1:2026-08-21", today)),
            hasMore = false,
            nextCursor = null,
            fresh = true,
            today = today,
        )

        assertEquals(1, again.total)
    }

    @Test
    fun `completion patch touches only the matching occurrence`() {
        val days = groupEntries(
            listOf(entry("event:1:2026-08-21", today), entry("event:2:2026-08-21", today)),
            today,
        )

        val patched = days.patchCompletion("event:2:2026-08-21", true)

        assertFalse(patched.first().entries.first().completed)
        assertTrue(patched.first().entries[1].completed)
    }

    @Test
    fun `removing the last occurrence of a day drops the day`() {
        val days = groupEntries(
            listOf(entry("event:1:2026-08-21", today), entry("event:2:2026-08-22", today.plusDays(1))),
            today,
        )

        assertEquals(listOf(today.plusDays(1)), days.withoutOccurrence("event:1:2026-08-21").map { it.date })
        assertEquals(listOf(today), days.withoutEvent(2).map { it.date })
    }

    @Test
    fun `relative captions exist only near today`() {
        assertEquals("hoje", dayCaption(today, today).second)
        assertEquals("amanhã", dayCaption(today.plusDays(1), today).second)
        assertNull(dayCaption(today.plusDays(30), today).second)
        assertEquals("qua, 26 ago", dayCaption(today.plusDays(5), today).first)
    }
}
