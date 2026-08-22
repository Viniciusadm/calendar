package com.archieapps.calendar.feature.settings

import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.design.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsModelTest {
    @Test
    fun `digest steps in half hours and wraps around midnight`() {
        assertEquals(8 * 60 + 30, nextDigestSlot(8 * 60))
        assertEquals(8 * 60, previousDigestSlot(8 * 60 + 30))
        assertEquals(0, nextDigestSlot(23 * 60 + 30))
        assertEquals(23 * 60 + 30, previousDigestSlot(0))
    }

    @Test
    fun `digest label is padded`() {
        assertEquals("08:00", Preferences(digestMinuteOfDay = 8 * 60).digestLabel)
        assertEquals("00:30", Preferences(digestMinuteOfDay = 30).digestLabel)
        assertEquals("21:30", Preferences(digestMinuteOfDay = 21 * 60 + 30).digestLabel)
    }

    @Test
    fun `theme maps to the design enum`() {
        assertEquals(ThemeMode.System, Preferences(themeMode = Settings.THEME_SYSTEM).theme())
        assertEquals(ThemeMode.Light, Preferences(themeMode = Settings.THEME_LIGHT).theme())
        assertEquals(ThemeMode.Dark, Preferences(themeMode = Settings.THEME_DARK).theme())
        assertEquals(ThemeMode.System, Preferences(themeMode = "lixo").theme())
    }

    @Test
    fun `week start maps to a choice value`() {
        assertEquals("sunday", Preferences(weekStartsMonday = false).weekStartValue)
        assertEquals("monday", Preferences(weekStartsMonday = true).weekStartValue)
    }
}
