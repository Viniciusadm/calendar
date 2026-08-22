package com.archieapps.calendar.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.archieapps.calendar.core.store.TaskSnapshotStore
import com.archieapps.calendar.core.store.WidgetRevision
import com.archieapps.calendar.core.sync.ReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onCompletionChanged(context: Context, occurrenceId: String, completed: Boolean) {
        val app = context.applicationContext

        scope.launch {
            TaskSnapshotStore(app).patchCompletion(occurrenceId, completed)
            redraw(app)
            ReminderSync.refreshWidgets(app)
        }
    }

    fun onWrote(context: Context) {
        val app = context.applicationContext

        scope.launch {
            TaskSnapshotStore(app).markStale()
            redraw(app)
            ReminderSync.refreshWidgets(app)
        }
    }

    fun onLookChanged(context: Context) {
        val app = context.applicationContext

        WidgetRevision.bump()
        scope.launch { redraw(app) }
    }

    private suspend fun redraw(context: Context) {
        runCatching { TodayTasksWidget().updateAll(context) }
    }
}
