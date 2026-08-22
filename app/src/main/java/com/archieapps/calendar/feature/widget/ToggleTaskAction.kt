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
import com.archieapps.calendar.core.store.WidgetRevision

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
        val widget = TodayTasksWidget()

        store.patchCompletion(id, target)
        widget.update(context, glanceId)

        val settings = Settings(context)

        if (!settings.isLoggedIn) {
            store.patchCompletion(id, wasCompleted)
            widget.update(context, glanceId)

            return
        }

        if (CalendarApi(settings).setCompletion(id, target) is ApiResult.Failure) {
            store.patchCompletion(id, wasCompleted)
        }

        WidgetRevision.bumpFromWidget()
        widget.updateAll(context)
    }

    companion object {
        val occurrenceId = ActionParameters.Key<String>("occurrenceId")

        val completed = ActionParameters.Key<Boolean>("completed")
    }
}
