package com.archieapps.calendar.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.archieapps.calendar.core.alarm.AlarmScheduler
import com.archieapps.calendar.core.alarm.Notifications
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.net.OccurrenceDto
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.store.TaskSnapshot
import com.archieapps.calendar.core.store.TaskSnapshotRow
import com.archieapps.calendar.core.store.TaskSnapshotStore
import com.archieapps.calendar.feature.calendar.toEntry
import com.archieapps.calendar.feature.calendar.togglable
import com.archieapps.calendar.feature.tasks.anchorCaption
import com.archieapps.calendar.feature.widget.TodayTasksWidget
import com.archieapps.calendar.feature.widget.WidgetConfig
import com.archieapps.calendar.feature.widget.WidgetConfigStore
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

    private const val MAX_SNAPSHOT_ROWS = 30

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

    suspend fun refreshWidgets(context: Context): Int {
        val settings = Settings(context)

        if (!settings.isLoggedIn) return 0

        val manager = GlanceAppWidgetManager(context)
        val ids = runCatching { manager.getGlanceIds(TodayTasksWidget::class.java) }.getOrDefault(emptyList())

        if (ids.isEmpty()) return 0

        val api = CalendarApi(settings)
        val configs = WidgetConfigStore(context)
        val snapshots = TaskSnapshotStore(context)
        val today = LocalDate.now()
        val overdue = when (val summary = api.taskSummary()) {
            is ApiResult.Ok -> summary.value.counts["overdue"] ?: 0
            is ApiResult.Failure -> 0
        }

        var refreshed = 0
        val alive = mutableSetOf<Int>()

        ids.forEach { glanceId ->
            val widgetId = runCatching { manager.getAppWidgetId(glanceId) }.getOrNull() ?: return@forEach
            val config = configs.load(widgetId)

            alive += widgetId

            val rows = when (
                val result = api.tasks(
                    filter = config.filter,
                    categories = config.categoriesParam,
                    priorities = config.prioritiesParam,
                    perPage = fetchSize(config),
                )
            ) {
                is ApiResult.Ok -> result.value
                is ApiResult.Failure -> return@forEach
            }

            val visible = rows
                .filter { config.showCompleted || !it.completed }
                .map { it.toSnapshotRow(today) }

            snapshots.save(
                widgetId,
                TaskSnapshot(
                    date = today.toString(),
                    rows = visible.take(config.maxRows),
                    overdue = overdue,
                    total = visible.size,
                ),
            )

            runCatching { TodayTasksWidget().update(context, glanceId) }
            refreshed++
        }

        snapshots.prune(alive)

        return refreshed
    }

    private fun fetchSize(config: WidgetConfig): Int =
        (config.maxRows * 2).coerceIn(config.maxRows, MAX_SNAPSHOT_ROWS)

    private fun OccurrenceDto.toSnapshotRow(today: LocalDate): TaskSnapshotRow {
        val entry = toEntry()

        return TaskSnapshotRow(
            occurrenceId = entry.id,
            title = entry.title,
            completed = entry.completed,
            clock = entry.clock,
            overdue = entry.overdue,
            recurring = entry.recurring,
            color = colorToken ?: color,
            priority = entry.priority,
            caption = anchorCaption(entry, today),
            togglable = entry.togglable(today),
            actionType = entry.action?.type,
            actionTarget = entry.action?.target,
            actionLabel = entry.action?.label,
        )
    }
}
