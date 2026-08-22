package com.archieapps.calendar.feature.tasks

import androidx.compose.ui.graphics.Color
import com.archieapps.calendar.feature.calendar.Agency
import com.archieapps.calendar.feature.calendar.CalendarEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskModelTest {
    private val today = LocalDate.of(2026, 8, 21)

    private fun task(
        id: String = "event:1:2026-08-21",
        date: LocalDate = today,
        title: String = "Tomar remédio",
        bucket: String? = "today",
        completed: Boolean = false,
        recurring: Boolean = false,
        recurrenceRule: String? = null,
        recurrenceEndsAt: LocalDate? = null,
        dueDate: LocalDate? = null,
        overdue: Boolean = false,
        streak: Int? = null,
        clock: String? = null,
    ): CalendarEntry = CalendarEntry(
        id = id,
        eventId = 1,
        title = title,
        date = date,
        clock = clock,
        allDay = clock == null,
        color = Color.Unspecified,
        agency = Agency.Mine,
        kind = "event",
        nature = "task",
        completed = completed,
        isTask = true,
        categoryName = null,
        note = null,
        description = null,
        endTime = null,
        durationMinutes = 1440,
        priority = "none",
        completedOn = null,
        overridden = false,
        remindersMuted = false,
        recurring = recurring,
        recurrenceRule = recurrenceRule,
        recurrenceEndsAt = recurrenceEndsAt,
        dueDate = dueDate ?: date,
        overdue = overdue,
        bucket = bucket,
        streak = streak,
        editable = true,
    )

    @Test
    fun `filter values match the api contract`() {
        assertEquals(
            listOf("pending", "today", "upcoming", "overdue", "all", "done"),
            TaskFilter.entries.map { it.value },
        )
        assertEquals(TaskFilter.Pending, TaskFilter.of(null))
        assertEquals(TaskFilter.Pending, TaskFilter.of("nope"))
        assertEquals(TaskFilter.Overdue, TaskFilter.of("overdue"))
        assertTrue(TaskFilter.Done.isHistory)
    }

    @Test
    fun `merge keeps order and drops rows already seen`() {
        val first = listOf(task(id = "a"), task(id = "b"))
        val second = listOf(task(id = "b"), task(id = "c"))

        assertEquals(listOf("a", "b", "c"), mergeRows(first, second, fresh = false).map { it.id })
        assertEquals(listOf("b", "c"), mergeRows(first, second, fresh = true).map { it.id })
    }

    @Test
    fun `completion patch touches only the target row`() {
        val rows = listOf(task(id = "a"), task(id = "b"))
        val patched = rows.patchRowCompletion("b", true)

        assertEquals(false, patched.first { it.id == "a" }.completed)
        assertEquals(true, patched.first { it.id == "b" }.completed)
    }

    @Test
    fun `removing a row and a whole series`() {
        val rows = listOf(task(id = "a"), task(id = "b"))

        assertEquals(listOf("a"), rows.withoutRow("b").map { it.id })
        assertTrue(rows.withoutSeries(1).isEmpty())
        assertEquals(2, rows.withoutSeries(null).size)
    }

    @Test
    fun `overdue anchors on the due date and reads as venceu`() {
        val entry = task(
            date = LocalDate.of(2026, 7, 12),
            bucket = "overdue",
            overdue = true,
        )

        assertEquals("venceu há 40 dias", anchorCaption(entry, today))
    }

    @Test
    fun `recurring upcoming reads as proxima`() {
        val entry = task(
            date = today.plusDays(3),
            bucket = "upcoming",
            recurring = true,
            recurrenceRule = "FREQ=WEEKLY;BYDAY=MO,WE,FR",
        )

        assertEquals("próxima em 3 dias", anchorCaption(entry, today))
        assertEquals("seg, qua e sex", recurrenceCaption(entry))
    }

    @Test
    fun `today anchors as hoje even when completed today`() {
        assertEquals("hoje", anchorCaption(task(bucket = "today"), today))
        assertEquals(
            "concluída hoje",
            anchorCaption(task(bucket = "settled", completed = true), today),
        )
    }

    @Test
    fun `deadline caption appears only when the due date differs`() {
        val sameDay = task(date = today, dueDate = today, bucket = "today")
        val later = task(date = today, dueDate = today.plusDays(3), bucket = "today")
        val late = task(date = today.minusDays(5), dueDate = today.minusDays(1), bucket = "overdue")

        assertNull(dueCaption(sameDay, today))
        assertEquals("vence em 3 dias", dueCaption(later, today))
        assertNull(dueCaption(late, today))
    }

    @Test
    fun `horizon caption only for recurring series with an end`() {
        val bounded = task(
            recurring = true,
            recurrenceRule = "FREQ=DAILY;UNTIL=20261130",
            recurrenceEndsAt = LocalDate.of(2026, 11, 30),
        )

        assertEquals("até seg, 30 nov", horizonCaption(bounded, today))
        assertNull(horizonCaption(task(recurring = false, recurrenceEndsAt = LocalDate.of(2026, 11, 30)), today))
    }

    @Test
    fun `streak shows only from two days on`() {
        assertNull(streakCaption(task(streak = 1)))
        assertNull(streakCaption(task(streak = null)))
        assertEquals("12 dias seguidos", streakCaption(task(streak = 12)))
    }

    @Test
    fun `state derives paging and filter counters`() {
        val state = TaskListState(
            rows = listOf(task(id = "a")),
            page = 1,
            lastPage = 3,
            query = "remedio",
            priorities = setOf("high"),
        )

        assertTrue(state.canLoadMore)
        assertEquals(2, state.activeFilterCount)
        assertEquals("high", state.prioritiesParam)
        assertNull(state.categoriesParam)
        assertTrue(!state.empty)
    }
}
