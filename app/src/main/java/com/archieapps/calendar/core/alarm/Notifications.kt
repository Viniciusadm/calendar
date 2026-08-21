package com.archieapps.calendar.core.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

object Notifications {
    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos dos seus eventos e tarefas."
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)
    }
}
