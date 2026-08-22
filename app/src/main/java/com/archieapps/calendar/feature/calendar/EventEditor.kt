package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.core.action.TaskActions
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.colorFromToken
import com.archieapps.calendar.feature.categories.pillLabel
import com.archieapps.calendar.design.components.CircleButton
import com.archieapps.calendar.design.components.DateStepper
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.PillRow
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.Section
import com.archieapps.calendar.design.components.Stepper
import com.archieapps.calendar.design.components.TextAction
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

private val durations = listOf(15 to "15 min", 30 to "30 min", 60 to "1 h", 90 to "1 h 30", 120 to "2 h")

@Composable
fun EventEditor(
    draft: EventDraft,
    categories: List<CategoryDto>,
    saving: Boolean,
    onChange: ((EventDraft) -> EventDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val context = LocalContext.current
    var pickingApp by remember { mutableStateOf(false) }

    if (pickingApp) {
        AppPickerDialog(
            onPick = { app ->
                pickingApp = false
                onChange { it.copy(actionTarget = app.packageName) }
            },
            onDismiss = { pickingApp = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Text(
            text = if (draft.isEditing) "editar" else "novo",
            style = MonthTitle,
            color = colors.ink,
        )

        Spacer(Modifier.height(Space.xl))

        HairlineField(
            value = draft.title,
            onValueChange = { value -> onChange { it.copy(title = value) } },
            label = "o quê",
            textStyle = SheetTitle,
        )

        Spacer(Modifier.height(Space.md))

        HairlineField(
            value = draft.description,
            onValueChange = { value -> onChange { it.copy(description = value) } },
            label = "detalhes",
            placeholder = "opcional",
        )

        Spacer(Modifier.height(Space.xl))
        Section("tipo")
        PillRow {
            Pill("evento", !draft.isTask, { onChange { it.copy(isTask = false) } })
            Pill("tarefa", draft.isTask, { onChange { it.copy(isTask = true) } })
        }

        Spacer(Modifier.height(Space.xl))
        Section("quando")
        DateStepper(draft.date, onPick = { date -> onChange { it.onDate(date) } })

        Spacer(Modifier.height(Space.md))
        PillRow {
            Pill("dia inteiro", draft.allDay, { onChange { it.copy(allDay = true) } })
            Pill("com horário", !draft.allDay, { onChange { it.copy(allDay = false) } })
        }

        if (!draft.allDay) {
            Spacer(Modifier.height(Space.md))
            HairlineField(
                value = draft.time,
                onValueChange = { value -> onChange { it.copy(time = value.take(5)) } },
                label = "hora (hh:mm)",
                placeholder = "19:00",
                keyboardType = KeyboardType.Number,
            )

            Spacer(Modifier.height(Space.md))
            Section("duração")
            ScrollingPills {
                durations.forEach { (minutes, label) ->
                    Pill(label, draft.durationMinutes == minutes, { onChange { it.copy(durationMinutes = minutes) } })
                }
            }
        }

        if (draft.isTask) {
            Spacer(Modifier.height(Space.xl))
            Section("prazo")

            PillRow {
                Pill("vence no dia", !draft.hasDeadline, { onChange { it.copy(dueOffsetDays = 0) } })
                Pill("outra data", draft.hasDeadline, { onChange { it.onDueDate(it.date.plusDays(1)) } })
            }

            if (draft.hasDeadline) {
                Spacer(Modifier.height(Space.md))

                DateStepper(
                    date = draft.dueDate,
                    onPick = { due -> onChange { it.onDueDate(due) } },
                    months = true,
                )

                Spacer(Modifier.height(Space.sm))

                Text(
                    text = deadlineCaption(draft),
                    style = EntryMeta,
                    color = colors.slate,
                )
            }

            Spacer(Modifier.height(Space.xl))
            Section("ação")

            PillRow {
                Pill("nenhuma", draft.actionType == null, {
                    onChange { it.copy(actionType = null, actionTarget = "", actionLabel = "") }
                })
                Pill("link", draft.actionType == ActionKind.Link, {
                    onChange { it.copy(actionType = ActionKind.Link, actionTarget = "") }
                })
                Pill("app", draft.actionType == ActionKind.App, {
                    onChange { it.copy(actionType = ActionKind.App, actionTarget = "") }
                })
            }

            if (draft.actionType == ActionKind.Link) {
                Spacer(Modifier.height(Space.md))

                HairlineField(
                    value = draft.actionTarget,
                    onValueChange = { value -> onChange { it.copy(actionTarget = value) } },
                    label = "link",
                    placeholder = "https://…",
                    keyboardType = KeyboardType.Uri,
                )
            }

            if (draft.actionType == ActionKind.App) {
                Spacer(Modifier.height(Space.md))

                val chosen = draft.actionTarget.takeIf { it.isNotBlank() }

                PillRow {
                    Pill(
                        label = chosen?.let { TaskActions.appLabel(context, it) ?: it } ?: "escolher aplicativo",
                        selected = chosen != null,
                        onClick = { pickingApp = true },
                    )
                }
            }

            if (draft.actionType != null) {
                Spacer(Modifier.height(Space.md))

                HairlineField(
                    value = draft.actionLabel,
                    onValueChange = { value -> onChange { it.copy(actionLabel = value) } },
                    label = "rótulo",
                    placeholder = "opcional",
                )
            }
        }

        Spacer(Modifier.height(Space.xl))
        RepeatFields(
            date = draft.date,
            rule = draft.recurrence,
            onDate = { date -> onChange { it.onDate(date) } },
            onRule = { transform -> onChange { it.withRecurrence(transform) } },
        )

        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(Space.xl))
            Section("categoria")
            ScrollingPills {
                categories.forEach { category ->
                    Pill(
                        label = category.pillLabel(),
                        selected = draft.categoryId == category.id,
                        onClick = { onChange { it.copy(categoryId = category.id) } },
                        dot = colorFromToken(category.color),
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xl))
        Section("prioridade")
        ScrollingPills {
            listOf("none" to "nenhuma", "low" to "baixa", "medium" to "média", "high" to "alta")
                .forEach { (value, label) ->
                    Pill(label, draft.priority == value, { onChange { it.copy(priority = value) } })
                }
        }

        Spacer(Modifier.height(Space.xl))
        Section("lembrete")
        ScrollingPills {
            reminderChoices.forEach { (label, minutes) ->
                Pill(label, draft.reminderMinutes == minutes, { onChange { it.copy(reminderMinutes = minutes) } })
            }
        }

        Spacer(Modifier.height(Space.xxl))

        Button(
            onClick = onSave,
            enabled = draft.canSave && !saving,
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

private fun deadlineCaption(draft: EventDraft): String {
    val days = draft.dueOffsetDays
    val span = if (days == 1) "1 dia depois" else "$days dias depois"

    return if (draft.recurrence.repeats) "$span de cada ocorrência" else span
}
