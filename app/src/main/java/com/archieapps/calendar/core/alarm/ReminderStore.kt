package com.archieapps.calendar.core.alarm

import android.content.Context
import com.archieapps.calendar.core.net.ReminderPlanEntry
import kotlinx.serialization.json.Json

class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("chronicle_alarms", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun save(plan: List<ReminderPlanEntry>) {
        prefs.edit()
            .putString(KEY_PLAN, json.encodeToString(plan))
            .putStringSet(KEY_KEYS, plan.map { it.key }.toSet())
            .apply()
    }

    fun load(): List<ReminderPlanEntry> {
        val raw = prefs.getString(KEY_PLAN, null) ?: return emptyList()

        return runCatching { json.decodeFromString<List<ReminderPlanEntry>>(raw) }.getOrDefault(emptyList())
    }

    fun scheduledKeys(): Set<String> = prefs.getStringSet(KEY_KEYS, emptySet()).orEmpty()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_PLAN = "plan"
        private const val KEY_KEYS = "keys"
    }
}
