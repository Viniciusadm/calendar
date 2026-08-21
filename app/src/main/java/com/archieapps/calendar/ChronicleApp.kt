package com.archieapps.calendar

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.archieapps.calendar.core.alarm.Notifications
import com.archieapps.calendar.core.sync.ReminderSyncWorker
import java.util.concurrent.TimeUnit

class ChronicleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Notifications.ensureChannel(this)

        val request = PeriodicWorkRequestBuilder<ReminderSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val SYNC_WORK = "reminder-sync"
    }
}
