package com.archieapps.calendar.core.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.archieapps.calendar.feature.calendar.TaskAction

object TaskActions {
    fun intentFor(context: Context, action: TaskAction): Intent? {
        val intent = if (action.isApp) {
            context.packageManager.getLaunchIntentForPackage(action.target)
        } else {
            runCatching { Intent(Intent.ACTION_VIEW, Uri.parse(action.target)) }.getOrNull()
        }

        return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun launch(context: Context, action: TaskAction): Boolean {
        val intent = intentFor(context, action) ?: return false

        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun appLabel(context: Context, packageName: String): String? = runCatching {
        val manager = context.packageManager

        manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    fun installedApps(context: Context): List<InstalledApp> {
        val manager = context.packageManager
        val probe = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return manager.queryIntentActivities(probe, 0)
            .mapNotNull { resolved ->
                val info = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null

                InstalledApp(
                    packageName = info.packageName,
                    label = manager.getApplicationLabel(info).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun icon(context: Context, packageName: String) = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()
}

data class InstalledApp(val packageName: String, val label: String)
