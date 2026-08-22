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
)

@Serializable
data class TaskSnapshot(
    val date: String = "",
    val rows: List<TaskSnapshotRow> = emptyList(),
    val overdue: Int = 0,
) {
    val pending: Int get() = rows.count { !it.completed }
}

class TaskSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("chronicle_tasks", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun save(snapshot: TaskSnapshot) {
        prefs.edit().putString(KEY_SNAPSHOT, json.encodeToString(snapshot)).apply()
    }

    fun load(): TaskSnapshot {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return TaskSnapshot()

        return runCatching { json.decodeFromString<TaskSnapshot>(raw) }.getOrDefault(TaskSnapshot())
    }

    fun patchCompletion(occurrenceId: String, completed: Boolean): TaskSnapshot {
        val current = load()
        val next = current.copy(
            rows = current.rows.map { row ->
                if (row.occurrenceId == occurrenceId) row.copy(completed = completed) else row
            }
        )

        save(next)

        return next
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_SNAPSHOT = "today"
    }
}
