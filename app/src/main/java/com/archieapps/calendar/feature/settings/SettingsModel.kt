package com.archieapps.calendar.feature.settings

import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.design.ThemeMode

val themeChoices: List<Pair<String, String>> = listOf(
    Settings.THEME_SYSTEM to "sistema",
    Settings.THEME_LIGHT to "claro",
    Settings.THEME_DARK to "escuro",
)

val weekStartChoices: List<Pair<String, String>> = listOf(
    "sunday" to "domingo",
    "monday" to "segunda",
)

val initialFilterChoices: List<Pair<String, String>> = listOf(
    "pending" to "pendentes",
    "today" to "hoje",
    "all" to "todas",
)

data class Preferences(
    val themeMode: String = Settings.THEME_SYSTEM,
    val weekStartsMonday: Boolean = false,
    val initialTaskFilter: String = "pending",
    val expandRecurringInGrid: Boolean = false,
    val digestEnabled: Boolean = false,
    val digestMinuteOfDay: Int = Settings.DEFAULT_DIGEST_AT,
    val notifyOverdue: Boolean = true,
) {
    val weekStartValue: String get() = if (weekStartsMonday) "monday" else "sunday"

    val digestLabel: String
        get() = "%02d:%02d".format(digestMinuteOfDay / 60, digestMinuteOfDay % 60)
}

fun Preferences.theme(): ThemeMode = when (themeMode) {
    Settings.THEME_LIGHT -> ThemeMode.Light
    Settings.THEME_DARK -> ThemeMode.Dark
    else -> ThemeMode.System
}

fun Settings.readPreferences(): Preferences = Preferences(
    themeMode = themeMode,
    weekStartsMonday = weekStartsMonday,
    initialTaskFilter = initialTaskFilter,
    expandRecurringInGrid = expandRecurringInGrid,
    digestEnabled = digestEnabled,
    digestMinuteOfDay = digestMinuteOfDay,
    notifyOverdue = notifyOverdue,
)

fun Settings.write(preferences: Preferences) {
    themeMode = preferences.themeMode
    weekStartsMonday = preferences.weekStartsMonday
    initialTaskFilter = preferences.initialTaskFilter
    expandRecurringInGrid = preferences.expandRecurringInGrid
    digestEnabled = preferences.digestEnabled
    digestMinuteOfDay = preferences.digestMinuteOfDay
    notifyOverdue = preferences.notifyOverdue
}

fun nextDigestSlot(current: Int, step: Int = 30): Int {
    val slots = (0 until 24 * 60 step step).toList()
    val index = slots.indexOfFirst { it > current }

    return if (index == -1) slots.first() else slots[index]
}

fun previousDigestSlot(current: Int, step: Int = 30): Int {
    val slots = (0 until 24 * 60 step step).toList()
    val index = slots.indexOfLast { it < current }

    return if (index == -1) slots.last() else slots[index]
}
