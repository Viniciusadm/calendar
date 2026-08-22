package com.archieapps.calendar.core.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object Notifications {
    const val CHANNEL_ID = "reminders"

    const val DIGEST_CHANNEL_ID = "digest"

    const val DIGEST_NOTIFICATION_ID = 91_001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { enableVibration(true) }
            )
        }

        if (manager.getNotificationChannel(DIGEST_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    DIGEST_CHANNEL_ID,
                    "Resumo do dia",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { enableVibration(false) }
            )
        }
    }
}
