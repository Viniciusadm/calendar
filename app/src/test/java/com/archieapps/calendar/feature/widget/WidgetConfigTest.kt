package com.archieapps.calendar.feature.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetConfigTest {
    @Test
    fun `no minimum priority means no filter at all`() {
        assertNull(WidgetConfig(minPriority = "none").prioritiesParam)
    }

    @Test
    fun `a minimum priority widens upward, never downward`() {
        assertEquals("low,medium,high", WidgetConfig(minPriority = "low").prioritiesParam)
        assertEquals("medium,high", WidgetConfig(minPriority = "medium").prioritiesParam)
        assertEquals("high", WidgetConfig(minPriority = "high").prioritiesParam)
    }

    @Test
    fun `an empty category list sends no category param`() {
        assertNull(WidgetConfig().categoriesParam)
    }

    @Test
    fun `chosen categories go as csv`() {
        assertEquals("3,7", WidgetConfig(categoryIds = listOf(3, 7)).categoriesParam)
    }

    @Test
    fun `the filter carries its own label for the header`() {
        assertEquals("hoje", WidgetConfig(filter = "today").filterLabel)
        assertEquals("atrasadas", WidgetConfig(filter = "overdue").filterLabel)
    }

    @Test
    fun `an unknown filter falls back to its raw value`() {
        assertEquals("banana", WidgetConfig(filter = "banana").filterLabel)
    }

    @Test
    fun `the default widget shows today with room for six rows`() {
        val config = WidgetConfig()

        assertEquals("today", config.filter)
        assertEquals(6, config.maxRows)
    }
}
