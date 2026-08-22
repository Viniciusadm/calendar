package com.archieapps.calendar.feature.calendar

import com.archieapps.calendar.core.net.EventDto
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class EventDraftTest {
    private val draft = EventDraft(title = "corrida", date = LocalDate.of(2026, 8, 12))

    @Test
    fun `payload omits the action when the entry is not a task`() {
        val withAction = draft.copy(actionType = ActionKind.Link, actionTarget = "https://a.com")

        assertEquals(JsonNull, withAction.toPayload()["actionType"])
        assertEquals(JsonNull, withAction.toPayload()["actionTarget"])
    }

    @Test
    fun `payload trims the target and drops a blank label`() {
        val task = draft.copy(
            isTask = true,
            actionType = ActionKind.Link,
            actionTarget = "  https://nubank.com.br  ",
            actionLabel = "   ",
        )

        val payload = task.toPayload()

        assertEquals("https://nubank.com.br", payload["actionTarget"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, payload["actionLabel"])
    }

    @Test
    fun `editing a task keeps the action it already had`() {
        val dto = EventDto(
            id = 7,
            title = "pagar aluguel",
            nature = "task",
            date = "2026-08-12",
            actionType = "app",
            actionTarget = "com.nu.production",
            actionLabel = "Nubank",
        )

        val payload = EventDraft.from(dto).toPayload()

        assertEquals("app", payload["actionType"]?.jsonPrimitive?.content)
        assertEquals("com.nu.production", payload["actionTarget"]?.jsonPrimitive?.content)
        assertEquals("Nubank", payload["actionLabel"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a task cannot be saved with an action that has no target`() {
        assertEquals(false, draft.copy(isTask = true, actionType = ActionKind.App).canSave)
    }

    @Test
    fun `payload clears the recurrence when the event does not repeat`() {
        assertEquals(JsonNull, draft.toPayload()["recurrence"])
    }

    @Test
    fun `payload carries the assembled rule`() {
        val repeating = draft.withUnit(RepeatUnit.Week)
            .withRecurrence { it.copy(interval = 2, weekdays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) }
            .withRecurrence { it.copy(ending = RepeatEnding.Count, count = 10) }

        assertEquals(
            "FREQ=WEEKLY;INTERVAL=2;BYDAY=WE,FR;COUNT=10",
            repeating.toPayload()["recurrence"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `choosing weekly preselects the weekday of the event`() {
        assertEquals(setOf(DayOfWeek.WEDNESDAY), draft.withUnit(RepeatUnit.Week).recurrence.weekdays)
    }

    @Test
    fun `moving the date keeps the monthly weekday pattern in sync`() {
        val monthly = draft.withUnit(RepeatUnit.Month)
            .withRecurrence { it.copy(monthlyMode = MonthlyMode.WeekdayOfMonth) }

        assertEquals("FREQ=MONTHLY;BYDAY=WE;BYSETPOS=2", monthly.toPayload()["recurrence"]?.jsonPrimitive?.content)

        val moved = monthly.onDate(LocalDate.of(2026, 8, 24))

        assertEquals("FREQ=MONTHLY;BYDAY=MO;BYSETPOS=4", moved.toPayload()["recurrence"]?.jsonPrimitive?.content)
    }
}
