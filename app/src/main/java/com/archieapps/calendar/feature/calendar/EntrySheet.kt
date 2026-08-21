package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.TextAction

@Composable
fun EntrySheet(
    entry: CalendarEntry,
    onEdit: () -> Unit,
    onToggleCompletion: () -> Unit,
    onCancelOccurrence: () -> Unit,
    onDeleteSeries: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Space.lg)) {
        Text(entry.title, style = SheetTitle, color = colors.ink)

        val meta = describe(entry)

        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            Text(meta, style = EntryMeta, color = colors.slate)
        }

        Spacer(Modifier.height(Space.md))

        if (entry.agency != Agency.Mine) {
            TextAction("Fechar", onDismiss, colors.slate, stretch = true)
            Spacer(Modifier.height(Space.sm))
            return@Column
        }

        if (entry.isTask) {
            TextAction(
                label = if (entry.completed) "Reabrir" else "Marcar como concluído",
                onClick = onToggleCompletion,
                color = colors.brand,
                stretch = true,
            )
        }

        TextAction("Editar evento", onEdit, colors.ink, stretch = true)

        if (entry.recurring) {
            TextAction("Remover só esta ocorrência", onCancelOccurrence, colors.ink, stretch = true)
            TextAction("Remover a série inteira", onDeleteSeries, colors.brand, stretch = true)
        } else {
            TextAction("Remover", onDeleteSeries, colors.brand, stretch = true)
        }

        TextAction("Fechar", onDismiss, colors.slate, stretch = true)

        Spacer(Modifier.height(Space.sm))
    }
}

private fun describe(entry: CalendarEntry): String = buildList {
    entry.clock?.let { add(it) }
    if (entry.allDay && entry.agency == Agency.Mine) add("dia inteiro")
    entry.categoryName?.let { add(it) }
    if (entry.recurring) add(entry.recurrenceRule?.let { Recurrence.parse(it).summary() } ?: "repete")
    if (entry.completed) add("concluído")
    entry.note?.let { add(it) }
}.joinToString(" · ")
