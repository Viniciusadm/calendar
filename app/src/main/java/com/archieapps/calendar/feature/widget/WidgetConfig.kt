package com.archieapps.calendar.feature.widget

import android.content.Context
import com.archieapps.calendar.core.store.WidgetRevision
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val widgetFilters: List<Pair<String, String>> = listOf(
    "today" to "hoje",
    "upcoming" to "próximas",
    "overdue" to "atrasadas",
    "all" to "todas",
)

val widgetPriorities: List<Pair<String, String>> = listOf(
    "none" to "qualquer",
    "low" to "baixa ou mais",
    "medium" to "média ou mais",
    "high" to "só alta",
)

@Serializable
data class WidgetConfig(
    val filter: String = "today",
    val categoryIds: List<Int> = emptyList(),
    val minPriority: String = "none",
    val maxRows: Int = 6,
    val showCompleted: Boolean = true,
    val showActions: Boolean = true,
) {
    val filterLabel: String
        get() = widgetFilters.firstOrNull { it.first == filter }?.second ?: filter

    val categoriesParam: String?
        get() = categoryIds.takeIf { it.isNotEmpty() }?.joinToString(",")

    val prioritiesParam: String?
        get() = when (minPriority) {
            "low" -> "low,medium,high"
            "medium" -> "medium,high"
            "high" -> "high"
            else -> null
        }
}

class WidgetConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("chronicle_widget", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun load(widgetId: Int): WidgetConfig {
        val raw = prefs.getString(key(widgetId), null) ?: return WidgetConfig()

        return runCatching { json.decodeFromString<WidgetConfig>(raw) }.getOrDefault(WidgetConfig())
    }

    fun save(widgetId: Int, config: WidgetConfig) {
        prefs.edit().putString(key(widgetId), json.encodeToString(config)).apply()
        WidgetRevision.bump()
    }

    fun forget(widgetId: Int) = prefs.edit().remove(key(widgetId)).apply()

    private fun key(widgetId: Int) = "config_$widgetId"
}
