package com.archieapps.calendar.core.action

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
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

    fun roundIcon(context: Context, packageName: String, sizePx: Int, dim: Boolean = false): Bitmap? {
        val key = "$packageName@$sizePx@$dim"

        synchronized(rounded) {
            if (rounded.containsKey(key)) return rounded[key]
        }

        val bitmap = icon(context, packageName)?.let { runCatching { it.circular(sizePx, dim) }.getOrNull() }

        synchronized(rounded) { rounded[key] = bitmap }

        return bitmap
    }

    private const val DIMMED_ALPHA = 90

    private val rounded = HashMap<String, Bitmap?>()

    private fun Drawable.circular(sizePx: Int, dim: Boolean): Bitmap {
        val source = toBitmap(sizePx, sizePx)
        val output = createBitmap(sizePx, sizePx)
        val radius = sizePx / 2f

        Canvas(output).drawCircle(
            radius,
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                if (dim) alpha = DIMMED_ALPHA
            },
        )

        return output
    }
}

data class InstalledApp(val packageName: String, val label: String)
