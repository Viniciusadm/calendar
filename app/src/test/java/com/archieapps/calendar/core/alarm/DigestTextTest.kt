package com.archieapps.calendar.core.alarm

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DigestTextTest {
    @Test
    fun `nothing pending and nothing late means no notification`() {
        assertNull(digestContent(emptyList(), pending = 0, overdue = 0, notifyOverdue = true))
        assertNull(digestContent(listOf("Academia"), pending = 0, overdue = 3, notifyOverdue = false))
    }

    @Test
    fun `head counts pending and late separately`() {
        val single = digestContent(listOf("Academia"), pending = 1, overdue = 1, notifyOverdue = true)

        assertEquals("1 tarefa para hoje · 1 atrasada", single?.title)

        val many = digestContent(listOf("Academia"), pending = 3, overdue = 2, notifyOverdue = true)

        assertEquals("3 tarefas para hoje · 2 atrasadas", many?.title)
    }

    @Test
    fun `late count is dropped when the user turned it off`() {
        val content = digestContent(listOf("Academia"), pending = 2, overdue = 5, notifyOverdue = false)

        assertEquals("2 tarefas para hoje", content?.title)
    }

    @Test
    fun `body lists titles and folds the rest`() {
        val titles = listOf("Um", "Dois", "Três", "Quatro", "Cinco", "Seis")
        val content = digestContent(titles, pending = 6, overdue = 0, notifyOverdue = true)

        assertEquals("Um · Dois · Três · Quatro · e mais 2", content?.body)
    }

    @Test
    fun `body degrades when only the late count is known`() {
        val content = digestContent(emptyList(), pending = 0, overdue = 2, notifyOverdue = true)

        assertEquals("2 atrasadas", content?.title)
        assertEquals("abra para ver o que ficou", content?.body)
    }

    @Test
    fun `trigger lands today when the slot is still ahead`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = ZonedDateTime.of(2026, 8, 21, 6, 0, 0, 0, zone)
        val at = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(nextTrigger(8 * 60, now)),
            zone,
        )

        assertEquals(2026, at.year)
        assertEquals(21, at.dayOfMonth)
        assertEquals(8, at.hour)
        assertEquals(0, at.minute)
    }

    @Test
    fun `trigger rolls to tomorrow once the slot passed`() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = ZonedDateTime.of(2026, 8, 21, 9, 30, 0, 0, zone)
        val at = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(nextTrigger(8 * 60, now)),
            zone,
        )

        assertEquals(22, at.dayOfMonth)
        assertEquals(8, at.hour)
    }
}
