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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
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
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.PillRow
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

        Spacer(Modifier.height(Space.xl))
        RepeatFields(draft, onChange)

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

@Composable
private fun RepeatFields(draft: EventDraft, onChange: ((EventDraft) -> EventDraft) -> Unit) {
    val colors = LocalChronicle.current
    val rule = draft.recurrence

    Section("repetir")
    ScrollingPills {
        Pill("não repete", !rule.repeats, { onChange { it.withUnit(null) } })

        RepeatUnit.entries.forEach { unit ->
            Pill(unit.one, rule.unit == unit, { onChange { it.withUnit(unit) } })
        }
    }

    val unit = rule.unit ?: return

    Spacer(Modifier.height(Space.lg))
    Section("a cada")
    Stepper(
        value = rule.interval,
        onChange = { value -> onChange { it.withRecurrence { current -> current.copy(interval = value) } } },
        unit = if (rule.interval == 1) unit.one else unit.many,
        min = 1,
        max = Recurrence.MAX_INTERVAL,
    )

    if (unit == RepeatUnit.Week) {
        Spacer(Modifier.height(Space.lg))
        Section("nos dias")
        ScrollingPills {
            weekOrder.forEach { day ->
                Pill(
                    label = weekdayShort(day),
                    selected = day in rule.weekdays,
                    onClick = {
                        onChange {
                            it.withRecurrence { current ->
                                val days = if (day in current.weekdays) {
                                    current.weekdays - day
                                } else {
                                    current.weekdays + day
                                }

                                current.copy(weekdays = days)
                            }
                        }
                    },
                )
            }
        }
    }

    if (unit == RepeatUnit.Month) {
        Spacer(Modifier.height(Space.lg))
        Section("no mês")
        ScrollingPills {
            Pill(
                label = "todo dia ${draft.date.dayOfMonth}",
                selected = rule.monthlyMode == MonthlyMode.DayOfMonth,
                onClick = { onChange { it.withRecurrence { c -> c.copy(monthlyMode = MonthlyMode.DayOfMonth) } } },
            )
            Pill(
                label = "toda ${ordinalLabel(ordinalOf(draft.date))} ${weekdayName(draft.date.dayOfWeek)}",
                selected = rule.monthlyMode == MonthlyMode.WeekdayOfMonth,
                onClick = { onChange { it.withRecurrence { c -> c.copy(monthlyMode = MonthlyMode.WeekdayOfMonth) } } },
            )
        }

        if (rule.monthlyMode == MonthlyMode.DayOfMonth && draft.date.dayOfMonth > 28) {
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "meses sem o dia ${draft.date.dayOfMonth} são pulados",
                style = EntryMeta,
                color = colors.slate,
            )
        }
    }

    Spacer(Modifier.height(Space.lg))
    Section("termina")
    ScrollingPills {
        Pill("nunca", rule.ending == RepeatEnding.Never, { onChange { it.withEnding(RepeatEnding.Never) } })
        Pill("após", rule.ending == RepeatEnding.Count, { onChange { it.withEnding(RepeatEnding.Count) } })
        Pill("em", rule.ending == RepeatEnding.Until, { onChange { it.withEnding(RepeatEnding.Until) } })
    }

    when (rule.ending) {
        RepeatEnding.Count -> {
            Spacer(Modifier.height(Space.md))
            Stepper(
                value = rule.count,
                onChange = { value -> onChange { it.withRecurrence { c -> c.copy(count = value) } } },
                unit = if (rule.count == 1) "vez" else "vezes",
                min = 1,
                max = Recurrence.MAX_COUNT,
            )
        }

        RepeatEnding.Until -> {
            Spacer(Modifier.height(Space.md))
            DateStepper(
                date = rule.until ?: Recurrence.defaultUntil(draft.date),
                onPick = { date -> onChange { it.withRecurrence { c -> c.copy(until = date) } } },
                months = true,
            )
        }

        RepeatEnding.Never -> Unit
    }

    Spacer(Modifier.height(Space.md))
    Text(rule.summary(draft.date), style = EntryMeta, color = colors.brand)
}

private fun EventDraft.withEnding(ending: RepeatEnding): EventDraft =
    withRecurrence { it.copy(ending = ending) }

@Composable
private fun Section(label: String) {
    val colors = LocalChronicle.current

    Column {
        Text(label.uppercase(), style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))
    }
}


