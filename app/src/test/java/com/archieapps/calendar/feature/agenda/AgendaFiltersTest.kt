package com.archieapps.calendar.feature.agenda

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgendaFiltersTest {
    @Test
    fun `defaults send no filter params`() {
        val filters = AgendaFilters()

        assertNull(filters.queryParam)
        assertNull(filters.categoriesParam)
        assertNull(filters.kindsParam)
        assertNull(filters.naturesParam)
        assertEquals(0, filters.activeCount)
        assertEquals(LocalDate.now(), filters.from)
    }

    @Test
    fun `a blank query is not a filter`() {
        assertNull(AgendaFilters(query = "   ").queryParam)
        assertEquals(0, AgendaFilters(query = "   ").activeCount)
    }

    @Test
    fun `categories serialize sorted as csv`() {
        assertEquals("2,7,9", AgendaFilters(categoryIds = setOf(9, 2, 7)).categoriesParam)
    }

    @Test
    fun `a narrowed kind set is sent, the full set is not`() {
        assertEquals("event", AgendaFilters(kinds = setOf("event")).kindsParam)
        assertNull(AgendaFilters(kinds = agendaKinds.map { it.first }.toSet()).kindsParam)
    }

    @Test
    fun `active count adds one per narrowed dimension`() {
        val filters = AgendaFilters(
            query = "dentista",
            categoryIds = setOf(3),
            kinds = setOf("event"),
        )

        assertEquals(3, filters.activeCount)
    }
}
