package com.archieapps.calendar.core.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService

object ExactAlarms {
    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        return context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: false
    }

    fun requestable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun request(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
    }
}
