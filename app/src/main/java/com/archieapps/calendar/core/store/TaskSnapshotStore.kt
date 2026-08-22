package com.archieapps.calendar.core.store

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TaskSnapshotRow(
    val occurrenceId: String,
    val title: String,
    val completed: Boolean,
    val clock: String? = null,
    val overdue: Boolean = false,
    val recurring: Boolean = false,
    val color: String? = null,
    val priority: String = "none",
    val caption: String? = null,
    val togglable: Boolean = true,
    val actionType: String? = null,
    val actionTarget: String? = null,
    val actionLabel: String? = null,
)

@Serializable
data class TaskSnapshot(
    val date: String = "",
    val rows: List<TaskSnapshotRow> = emptyList(),
    val overdue: Int = 0,
    val total: Int = 0,
    val stale: Boolean = false,
) {
    val pending: Int get() = rows.count { !it.completed }
}

class TaskSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("chronicle_tasks", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun save(widgetId: Int, snapshot: TaskSnapshot) {
        prefs.edit().putString(key(widgetId), json.encodeToString(snapshot)).apply()
        WidgetRevision.bump()
    }

    fun load(widgetId: Int): TaskSnapshot = decode(prefs.getString(key(widgetId), null))

    fun forget(widgetId: Int) {
        prefs.edit().remove(key(widgetId)).apply()
        WidgetRevision.bump()
    }

    fun patchCompletion(occurrenceId: String, completed: Boolean) {
        val editor = prefs.edit()

        prefs.all.keys.filter { it.startsWith(KEY_PREFIX) }.forEach { key ->
            val current = decode(prefs.getString(key, null))

            if (current.rows.none { it.occurrenceId == occurrenceId }) {
                return@forEach
            }

            val next = current.copy(
                rows = current.rows.map { row ->
                    if (row.occurrenceId == occurrenceId) row.copy(completed = completed) else row
                }
            )

            editor.putString(key, json.encodeToString(next))
        }

        editor.apply()
        WidgetRevision.bump()
    }

    fun markStale() {
        val editor = prefs.edit()

        prefs.all.keys.filter { it.startsWith(KEY_PREFIX) }.forEach { key ->
            val current = decode(prefs.getString(key, null))

            editor.putString(key, json.encodeToString(current.copy(stale = true)))
        }

        editor.apply()
        WidgetRevision.bump()
    }

    fun prune(keep: Set<Int>) {
        val wanted = keep.map(::key).toSet()
        val gone = prefs.all.keys.filterNot { wanted.contains(it) }

        if (gone.isEmpty()) return

        val editor = prefs.edit()

        gone.forEach(editor::remove)
        editor.apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun decode(raw: String?): TaskSnapshot {
        if (raw == null) return TaskSnapshot()

        return runCatching { json.decodeFromString<TaskSnapshot>(raw) }.getOrDefault(TaskSnapshot())
    }

    private fun key(widgetId: Int) = KEY_PREFIX + widgetId

    private companion object {
        const val KEY_PREFIX = "today_"
    }
}
