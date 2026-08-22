package com.archieapps.calendar.core.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.archieapps.calendar.feature.widget.TodayTasksWidget

class ReminderSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(ReminderSync.KEY_FORCE, false)
        val outcome = ReminderSync.run(applicationContext, force)

        refreshWidget()

        return when (outcome) {
            is SyncOutcome.Failed -> Result.retry()
            else -> Result.success()
        }
    }

    private suspend fun refreshWidget() {
        if (ReminderSync.refreshTaskSnapshot(applicationContext) == null) {
            return
        }

        runCatching { TodayTasksWidget().updateAll(applicationContext) }
    }
}
