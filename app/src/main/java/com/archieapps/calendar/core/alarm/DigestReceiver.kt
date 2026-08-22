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
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DigestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                deliver(context)
            } finally {
                DigestScheduler(context).sync()
                pending.finish()
            }
        }
    }

    private suspend fun deliver(context: Context) {
        val settings = Settings(context)

        if (!settings.digestEnabled || !settings.isLoggedIn) {
            return
        }

        val api = CalendarApi(settings)
        val summary = api.taskSummary()

        if (summary !is ApiResult.Ok) {
            return
        }

        val rows = api.tasks(filter = "today", perPage = 20)
        val titles = if (rows is ApiResult.Ok) {
            rows.value.filterNot { it.completed }.map { it.title }
        } else {
            emptyList()
        }

        val content = digestContent(
            titles = titles,
            pending = summary.value.day.pending,
            overdue = summary.value.counts["overdue"] ?: 0,
            notifyOverdue = settings.notifyOverdue,
        ) ?: return

        notify(context, content)
    }

    private fun notify(context: Context, content: DigestContent) {
        Notifications.ensureChannel(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val open = PendingIntent.getActivity(
            context,
            Notifications.DIGEST_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Notifications.DIGEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(
            Notifications.DIGEST_NOTIFICATION_ID,
            notification,
        )
    }
}
