package com.archieapps.calendar.core.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.archieapps.calendar.MainActivity
import com.archieapps.calendar.R
import com.archieapps.calendar.core.net.ReminderPlanEntry
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        Notifications.ensureChannel(context)

        if (!canNotify(context)) return

        val open = PendingIntent.getActivity(
            context,
            key.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
    }

    private fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val EXTRA_KEY = "key"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"

        fun intentFor(context: Context, entry: ReminderPlanEntry): Intent =
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_KEY, entry.key)
                putExtra(EXTRA_TITLE, entry.title)
                putExtra(EXTRA_BODY, describe(entry))
            }

        private fun describe(entry: ReminderPlanEntry): String {
            val when_ = runCatching {
                OffsetDateTime.parse(entry.occurrenceStartAt).format(DateTimeFormatter.ofPattern("HH:mm"))
            }.getOrNull()

            val lead = when {
                entry.label != null -> entry.label
                entry.minutesBefore == 0 -> "agora"
                entry.minutesBefore < 60 -> "em ${entry.minutesBefore} min"
                entry.minutesBefore < 1440 -> "em ${entry.minutesBefore / 60} h"
                else -> "em ${entry.minutesBefore / 1440} dia(s)"
            }

            return listOfNotNull(
                lead,
                if (entry.allDay) "dia inteiro" else when_,
                entry.categoryName,
            ).joinToString(" · ")
        }
    }
}
