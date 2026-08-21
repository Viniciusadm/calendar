package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space

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

    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Space.lg)) {
        Text(entry.title, style = SheetTitle, color = colors.ink)

        Spacer(Modifier.height(Space.sm))

        Text(
            text = describe(entry),
            style = EntryMeta,
            color = colors.slate,
        )

        Spacer(Modifier.height(Space.xl))

        if (entry.agency != Agency.Mine) {
            Action("Fechar", colors.slate, onDismiss)
            Spacer(Modifier.height(Space.xl))
            return@Column
        }

        if (entry.isTask) {
            Action(
                label = if (entry.completed) "Reabrir" else "Marcar como concluído",
                color = colors.brand,
                onClick = onToggleCompletion,
            )
        }

        Action("Editar evento", colors.ink, onEdit)

        if (entry.recurring) {
            Action("Remover só esta ocorrência", colors.ink, onCancelOccurrence)
            Action("Remover a série inteira", colors.brand, onDeleteSeries)
        } else {
            Action("Remover", colors.brand, onDeleteSeries)
        }

        Action("Fechar", colors.slate, onDismiss)
        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
private fun Action(label: String, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = ButtonLabel, color = color, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
    }
}

private fun describe(entry: CalendarEntry): String = buildList {
    entry.clock?.let { add(it) }
    if (entry.allDay && entry.agency == Agency.Mine) add("dia inteiro")
    entry.categoryName?.let { add(it) }
    if (entry.recurring) add("repete")
    if (entry.completed) add("concluído")
    entry.note?.let { add(it) }
}.joinToString(" · ")

