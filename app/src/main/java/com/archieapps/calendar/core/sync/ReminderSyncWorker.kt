package com.archieapps.calendar.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(ReminderSync.KEY_FORCE, false)
        val outcome = ReminderSync.run(applicationContext, force)

        ReminderSync.refreshWidgets(applicationContext)

        return when (outcome) {
            is SyncOutcome.Failed -> Result.retry()
            else -> Result.success()
        }
    }
}
