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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.colorFromToken
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.PillRow
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
            placeholder = "Pilates, consulta, aniversário…",
            textStyle = MonthTitle.copy(fontSize = 22.sp, letterSpacing = (-0.6).sp),
        )

        Spacer(Modifier.height(Space.xl))
        Section("tipo")
        PillRow {
            Pill("evento", !draft.isTask, { onChange { it.copy(isTask = false) } })
            Pill("tarefa", draft.isTask, { onChange { it.copy(isTask = true) } })
        }

        Spacer(Modifier.height(Space.xl))
        Section("quando")
        DateRow(draft.date, onShift = { days -> onChange { it.copy(date = it.date.plusDays(days)) } })

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
        Section("repetir")
        ScrollingPills {
            Repeat.entries.forEach { option ->
                Pill(option.label, draft.repeat == option, { onChange { it.copy(repeat = option) } })
            }
        }

        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(Space.xl))
            Section("categoria")
            ScrollingPills {
                categories.forEach { category ->
                    Pill(
                        label = category.name,
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
            Text(if (saving) "Salvando…" else "Salvar", style = Eyebrow)
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar", style = Eyebrow, color = colors.slate)
        }

        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
private fun Section(label: String) {
    val colors = LocalChronicle.current

    Column {
        Text(label.uppercase(), style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun ScrollingPills(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun DateRow(date: LocalDate, onShift: (Long) -> Unit) {
    val colors = LocalChronicle.current
    val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, ptBr)
    val month = date.month.getDisplayName(JavaTextStyle.FULL, ptBr)

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onShift(-1) }) {
            Text("−1 dia", style = Eyebrow, color = colors.slate)
        }

        Text(
            text = "$weekday, ${date.dayOfMonth} de $month",
            style = EntryMeta,
            color = colors.ink,
            modifier = Modifier.padding(horizontal = Space.sm),
        )

        TextButton(onClick = { onShift(1) }) {
            Text("+1 dia", style = Eyebrow, color = colors.slate)
        }
    }
}
