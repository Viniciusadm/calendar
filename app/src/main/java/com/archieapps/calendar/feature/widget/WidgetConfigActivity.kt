package com.archieapps.calendar.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.sync.ReminderSync
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.CalendarTheme
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.colorFromToken
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.PillRow
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.Section
import com.archieapps.calendar.design.components.Stepper
import com.archieapps.calendar.design.components.SwitchRow
import com.archieapps.calendar.design.components.TextAction
import com.archieapps.calendar.feature.categories.pillLabel
import com.archieapps.calendar.feature.settings.readPreferences
import com.archieapps.calendar.feature.settings.theme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, resultIntent())

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()

            return
        }

        setContent {
            val context = LocalContext.current
            val settings = remember { Settings(context) }
            val store = remember { WidgetConfigStore(context) }
            var config by remember { mutableStateOf(store.load(widgetId)) }
            var categories by remember { mutableStateOf(emptyList<CategoryDto>()) }
            var saving by remember { mutableStateOf(false) }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (!settings.isLoggedIn) return@LaunchedEffect

                when (val result = CalendarApi(settings).categories()) {
                    is ApiResult.Ok -> categories = result.value
                    is ApiResult.Failure -> Unit
                }
            }

            CalendarTheme(mode = settings.readPreferences().theme()) {
                WidgetConfigScreen(
                    config = config,
                    categories = categories,
                    saving = saving,
                    onChange = { transform -> config = transform(config) },
                    onSave = {
                        saving = true
                        store.save(widgetId, config)
                        commit()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }

    private fun commit() {
        lifecycleScope.launch {
            ReminderSync.refreshWidgets(applicationContext)

            runCatching {
                val glanceId = GlanceAppWidgetManager(applicationContext).getGlanceIdBy(widgetId)

                TodayTasksWidget().update(applicationContext, glanceId)
            }.onFailure {
                runCatching { TodayTasksWidget().updateAll(applicationContext) }
            }

            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}

@Composable
private fun WidgetConfigScreen(
    config: WidgetConfig,
    categories: List<CategoryDto>,
    saving: Boolean,
    onChange: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.huge))

        Text(text = "widget", style = MonthTitle, color = colors.ink)

        Spacer(Modifier.height(Space.xl))
        Section("mostrar")
        ScrollingPills {
            widgetFilters.forEach { (value, label) ->
                Pill(label, config.filter == value, { onChange { it.copy(filter = value) } })
            }
        }

        Spacer(Modifier.height(Space.xl))
        Section("prioridade mínima")
        ScrollingPills {
            widgetPriorities.forEach { (value, label) ->
                Pill(label, config.minPriority == value, { onChange { it.copy(minPriority = value) } })
            }
        }

        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(Space.xl))
            Section("categorias")

            PillRow {
                Pill(
                    label = "todas",
                    selected = config.categoryIds.isEmpty(),
                    onClick = { onChange { it.copy(categoryIds = emptyList()) } },
                )
            }

            Spacer(Modifier.height(Space.sm))

            ScrollingPills {
                categories.forEach { category ->
                    Pill(
                        label = category.pillLabel(),
                        selected = config.categoryIds.contains(category.id),
                        onClick = {
                            onChange { current ->
                                val next = if (current.categoryIds.contains(category.id)) {
                                    current.categoryIds - category.id
                                } else {
                                    current.categoryIds + category.id
                                }

                                current.copy(categoryIds = next)
                            }
                        },
                        dot = colorFromToken(category.color),
                    )
                }
            }

            Spacer(Modifier.height(Space.sm))

            Text(
                text = if (config.categoryIds.isEmpty()) {
                    "sem filtro de categoria"
                } else {
                    "${config.categoryIds.size} selecionada(s)"
                },
                style = EntryMeta,
                color = colors.slate,
            )
        }

        Spacer(Modifier.height(Space.xl))
        Section("linhas")
        Stepper(
            value = config.maxRows,
            onChange = { rows -> onChange { it.copy(maxRows = rows) } },
            unit = "linhas",
            min = 1,
            max = 12,
        )

        Spacer(Modifier.height(Space.xl))

        SwitchRow(
            label = "mostrar concluídas",
            checked = config.showCompleted,
            onToggle = { value -> onChange { it.copy(showCompleted = value) } },
            caption = "quando desligado, a tarefa sai da lista ao ser marcada",
        )

        SwitchRow(
            label = "mostrar botão de ação",
            checked = config.showActions,
            onToggle = { value -> onChange { it.copy(showActions = value) } },
            caption = "atalho para o link ou aplicativo da tarefa",
        )

        Spacer(Modifier.height(Space.xxl))

        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand),
        ) {
            Text(if (saving) "Salvando…" else "Salvar", style = ButtonLabel)
        }

        Spacer(Modifier.height(Space.sm))

        TextAction(
            label = "Cancelar",
            onClick = onCancel,
            color = colors.slate,
            stretch = true,
            align = Alignment.Center,
        )

        Spacer(Modifier.height(Space.xl))
    }
}
