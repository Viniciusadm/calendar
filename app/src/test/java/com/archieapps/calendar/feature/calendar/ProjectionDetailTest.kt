package com.archieapps.calendar.feature.calendar

import com.archieapps.calendar.core.net.ProjectionDetailDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionDetailTest {
    private val today = LocalDate.of(2026, 8, 21)

    private fun birthday(
        name: String = "Guilherme Santana Filho",
        nickname: String? = "Guilherme",
        note: String? = null,
        instagram: String? = null,
        phone: String? = null,
        locked: Boolean = false,
    ) = ProjectionDetailDto(
        id = "birthday:282:2026-09-01",
        kind = "birthday",
        locked = locked,
        name = name,
        nickname = nickname,
        birthDate = "1999-09-01",
        age = 27,
        note = note,
        instagram = instagram,
        phone = phone,
    )

    private fun episode(
        episode: Int? = 20,
        total: Int? = 24,
        watched: Int? = 6,
        durationMinutes: Int? = 24,
        type: String? = "anime",
    ) = ProjectionDetailDto(
        id = "episode:13205:2026-08-21",
        kind = "episode",
        series = "Tensei Shitara Slime Datta Ken - 4ª temporada",
        episode = episode,
        total = total,
        watched = watched,
        durationMinutes = durationMinutes,
        type = type,
    )

    @Test
    fun `birthday shows the full name when the title used the nickname`() {
        val detail = birthday().toDetail(LocalDate.of(2026, 9, 1), today)

        assertEquals("Guilherme Santana Filho", detail.facts.first())
    }

    @Test
    fun `birthday hides the full name when it is the nickname`() {
        val detail = birthday(name = "Elisa", nickname = "Elisa").toDetail(LocalDate.of(2026, 9, 1), today)

        assertEquals(listOf("nasceu em 1 de set de 1999", "terça · em 11 dias"), detail.facts)
    }

    @Test
    fun `birthday dates the birth and the day it falls on`() {
        val detail = birthday().toDetail(LocalDate.of(2026, 9, 1), today)

        assertEquals("nasceu em 1 de set de 1999", detail.facts[1])
        assertEquals("terça · em 11 dias", detail.facts[2])
    }

    @Test
    fun `birthday carries note and contacts when they came`() {
        val detail = birthday(note = "amigo da facul", instagram = "santana.gui3", phone = "61994270578")
            .toDetail(LocalDate.of(2026, 9, 1), today)

        assertEquals("amigo da facul", detail.note)
        assertEquals(listOf("@santana.gui3", "61994270578"), detail.contacts)
        assertFalse(detail.locked)
    }

    @Test
    fun `a blank note is not a note`() {
        val detail = birthday(note = "   ").toDetail(LocalDate.of(2026, 9, 1), today)

        assertNull(detail.note)
    }

    @Test
    fun `locked birthday keeps the flag and has nothing private`() {
        val detail = birthday(locked = true).toDetail(LocalDate.of(2026, 9, 1), today)

        assertTrue(detail.locked)
        assertNull(detail.note)
        assertTrue(detail.contacts.isEmpty())
        assertFalse(detail.isEmpty)
    }

    @Test
    fun `episode joins position duration and type in one line`() {
        val detail = episode().toDetail(today, today)

        assertEquals(listOf("episódio 20 de 24 · 24 min · anime"), detail.facts)
    }

    @Test
    fun `episode without total omits the position denominator`() {
        val detail = episode(total = null, watched = null).toDetail(today, today)

        assertEquals(listOf("episódio 20 · 24 min · anime"), detail.facts)
        assertNull(detail.progress)
    }

    @Test
    fun `episode progress counts watched against the series total`() {
        val progress = episode().toDetail(today, today).progress

        assertEquals(6, progress?.done)
        assertEquals(24, progress?.total)
        assertEquals("6 de 24 assistidos", progress?.label)
        assertEquals(0.25f, progress?.fraction)
    }

    @Test
    fun `progress fraction never leaves the unit range`() {
        val over = episode(watched = 30).toDetail(today, today).progress

        assertEquals(1f, over?.fraction)
    }

    @Test
    fun `an unknown kind renders nothing`() {
        val detail = ProjectionDetailDto(id = "question:8:2026-08-21", kind = "question").toDetail(today, today)

        assertTrue(detail.isEmpty)
    }
}
