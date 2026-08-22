package com.archieapps.calendar.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.store.TaskSnapshotStore
import com.archieapps.calendar.core.sync.ReminderSync

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val id = parameters[occurrenceId] ?: return
        val wasCompleted = parameters[completed] ?: false
        val target = !wasCompleted
        val store = TaskSnapshotStore(context)

        store.patchCompletion(id, target)
        TodayTasksWidget().updateAll(context)

        val settings = Settings(context)

        if (!settings.isLoggedIn) {
            store.patchCompletion(id, wasCompleted)
            TodayTasksWidget().updateAll(context)

            return
        }

        when (CalendarApi(settings).setCompletion(id, target)) {
            is ApiResult.Ok -> ReminderSync.refreshTaskSnapshot(context)
            is ApiResult.Failure -> store.patchCompletion(id, wasCompleted)
        }

        TodayTasksWidget().updateAll(context)
    }

    companion object {
        val occurrenceId = ActionParameters.Key<String>("occurrenceId")

        val completed = ActionParameters.Key<Boolean>("completed")
    }
}
