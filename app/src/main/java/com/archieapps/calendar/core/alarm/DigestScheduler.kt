package com.archieapps.calendar.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.archieapps.calendar.core.store.Settings
import java.time.LocalTime
import java.time.ZoneId

class DigestScheduler(private val context: Context) {
    fun sync() {
        val settings = Settings(context)

        if (!settings.digestEnabled || !settings.isLoggedIn) {
            cancel()

            return
        }

        schedule(nextTrigger(settings.digestMinuteOfDay))
    }

    private fun schedule(triggerAt: Long) {
        val manager = context.getSystemService<AlarmManager>() ?: return

        Notifications.ensureChannel(context)

        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent())
        }
    }

    private fun cancel() {
        val manager = context.getSystemService<AlarmManager>() ?: return

        runCatching { manager.cancel(pendingIntent()) }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DigestReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 91_001
    }
}

fun nextTrigger(
    minuteOfDay: Int,
    now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(ZoneId.systemDefault()),
): Long {
    val slot = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    val time = LocalTime.of(slot / 60, slot % 60)
    val todayAt = now.with(time).withSecond(0).withNano(0)
    val target = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)

    return target.toInstant().toEpochMilli()
}

