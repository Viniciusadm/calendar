package com.archieapps.calendar.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.archieapps.calendar.core.alarm.AlarmScheduler
import com.archieapps.calendar.core.alarm.Notifications
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.store.TaskSnapshot
import com.archieapps.calendar.core.store.TaskSnapshotRow
import com.archieapps.calendar.core.store.TaskSnapshotStore
import java.time.LocalDate

sealed interface SyncOutcome {
    data object Unchanged : SyncOutcome
    data class Applied(val scheduled: Int) : SyncOutcome
    data class Failed(val message: String) : SyncOutcome
}

object ReminderSync {
    const val KEY_FORCE = "force"

    private const val WINDOW_DAYS = 30

    private const val NOW_WORK = "reminder-sync-now"

    private const val SNAPSHOT_ROWS = 12

    fun enqueue(context: Context, force: Boolean) {
        val request = OneTimeWorkRequestBuilder<ReminderSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf(KEY_FORCE to force))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun run(context: Context, force: Boolean = false): SyncOutcome {
        val settings = Settings(context)

        if (!settings.isLoggedIn) return SyncOutcome.Unchanged

        val api = CalendarApi(settings)

        val revision = when (val state = api.syncState()) {
            is ApiResult.Ok -> state.value.revision
            is ApiResult.Failure -> return SyncOutcome.Failed(state.message)
        }

        if (!force && revision == settings.revision) return SyncOutcome.Unchanged

        val plan = when (val result = api.reminderSchedule(LocalDate.now().toString(), WINDOW_DAYS)) {
            is ApiResult.Ok -> result.value
            is ApiResult.Failure -> return SyncOutcome.Failed(result.message)
        }

        Notifications.ensureChannel(context)
        AlarmScheduler(context).apply(plan)
        settings.revision = revision

        return SyncOutcome.Applied(plan.size)
    }

    suspend fun refreshTaskSnapshot(context: Context): TaskSnapshot? {
        val settings = Settings(context)

        if (!settings.isLoggedIn) return null

        val api = CalendarApi(settings)
        val today = LocalDate.now().toString()

        val rows = when (val result = api.tasks(filter = "today", perPage = SNAPSHOT_ROWS)) {
            is ApiResult.Ok -> result.value
            is ApiResult.Failure -> return null
        }

        val overdue = when (val summary = api.taskSummary()) {
            is ApiResult.Ok -> summary.value.counts["overdue"] ?: 0
            is ApiResult.Failure -> 0
        }

        val snapshot = TaskSnapshot(
            date = today,
            rows = rows.map { row ->
                TaskSnapshotRow(
                    occurrenceId = row.id,
                    title = row.title,
                    completed = row.completed,
                    clock = row.time,
                    overdue = row.overdue,
                    recurring = row.recurring,
                )
            },
            overdue = overdue,
        )

        TaskSnapshotStore(context).save(snapshot)

        return snapshot
    }
}
