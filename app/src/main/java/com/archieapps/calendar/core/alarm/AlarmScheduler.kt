package com.archieapps.calendar.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.archieapps.calendar.core.net.ReminderPlanEntry
import java.time.Instant
import java.time.OffsetDateTime

class AlarmScheduler(private val context: Context) {
    private val store = ReminderStore(context)

    fun apply(plan: List<ReminderPlanEntry>) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        val now = Instant.now()

        store.scheduledKeys().forEach { key -> cancel(manager, key) }

        val upcoming = plan.filter { entry ->
            runCatching { instantOf(entry).isAfter(now) }.getOrDefault(false)
        }

        upcoming.forEach { entry -> schedule(manager, entry) }

        store.save(upcoming)
    }

    fun rescheduleFromCache() = apply(store.load())

    private fun schedule(manager: AlarmManager, entry: ReminderPlanEntry) {
        val at = runCatching { instantOf(entry).toEpochMilli() }.getOrNull() ?: return
        val intent = ReminderReceiver.intentFor(context, entry)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(entry.key),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

        if (exact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun cancel(manager: AlarmManager, key: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(key),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        manager.cancel(pending)
        pending.cancel()
    }

    private fun instantOf(entry: ReminderPlanEntry): Instant =
        OffsetDateTime.parse(entry.triggerAt).toInstant()

    private fun requestCode(key: String): Int = key.hashCode()
}
