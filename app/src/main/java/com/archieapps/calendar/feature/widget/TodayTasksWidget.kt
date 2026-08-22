package com.archieapps.calendar.feature.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.archieapps.calendar.MainActivity
import com.archieapps.calendar.core.store.TaskSnapshot
import com.archieapps.calendar.core.store.TaskSnapshotStore

private val ground = ColorProvider(day = Color(0xFFFAFCFE), night = Color(0xFF0A131A))
private val ink = ColorProvider(day = Color(0xFF081822), night = Color(0xFFEFF2F5))
private val slate = ColorProvider(day = Color(0xFF59656E), night = Color(0xFF8E9AA4))
private val brand = ColorProvider(day = Color(0xFF349EF4), night = Color(0xFF349EF4))
private val destructive = ColorProvider(day = Color(0xFFE62B34), night = Color(0xFFFF6B6B))

private const val maxRows = 6

class TodayTasksWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = TaskSnapshotStore(context).load()

        provideContent {
            Body(snapshot)
        }
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()
}

@androidx.compose.runtime.Composable
private fun Body(snapshot: TaskSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "HOJE",
                style = TextStyle(color = slate, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )

            Spacer(GlanceModifier.width(8.dp))

            Text(
                text = headline(snapshot),
                style = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        if (snapshot.rows.isEmpty()) {
            Text(
                text = "nada para hoje",
                style = TextStyle(color = slate, fontSize = 14.sp),
            )
        }

        snapshot.rows.take(maxRows).forEach { row ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable(
                        actionRunCallback<ToggleTaskAction>(
                            actionParametersOf(
                                ToggleTaskAction.occurrenceId to row.occurrenceId,
                                ToggleTaskAction.completed to row.completed,
                            )
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (row.completed) "●" else "○",
                    style = TextStyle(color = if (row.completed) slate else brand, fontSize = 15.sp),
                )

                Spacer(GlanceModifier.width(8.dp))

                Text(
                    text = row.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (row.completed) slate else ink,
                        fontSize = 14.sp,
                        textDecoration = if (row.completed) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )

                row.clock?.let { clock ->
                    Spacer(GlanceModifier.width(8.dp))
                    Text(text = clock, style = TextStyle(color = slate, fontSize = 12.sp))
                }
            }
        }

        if (snapshot.rows.size > maxRows) {
            Text(
                text = "e mais ${snapshot.rows.size - maxRows}",
                style = TextStyle(color = slate, fontSize = 12.sp),
            )
        }

        if (snapshot.overdue > 0) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = overdueLine(snapshot.overdue),
                style = TextStyle(color = destructive, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

private fun headline(snapshot: TaskSnapshot): String {
    val pending = snapshot.pending

    return when {
        snapshot.rows.isEmpty() -> "livre"
        pending == 0 -> "tudo feito"
        pending == 1 -> "1 pendente"
        else -> "$pending pendentes"
    }
}

private fun overdueLine(count: Int): String =
    if (count == 1) "1 atrasada" else "$count atrasadas"
