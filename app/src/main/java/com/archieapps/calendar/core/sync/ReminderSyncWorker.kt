package com.archieapps.calendar.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (ReminderSync.run(applicationContext)) {
        is SyncOutcome.Failed -> Result.retry()
        else -> Result.success()
    }
}
