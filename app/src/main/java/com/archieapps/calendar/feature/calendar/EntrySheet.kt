package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.components.Pill
import java.time.LocalDate

private val priorityLabels = mapOf("low" to "baixa", "medium" to "média", "high" to "alta")

@Composable
fun EntrySheet(
    entry: CalendarEntry,
    detail: DetailState,
    onEdit: () -> Unit,
    onToggleCompletion: () -> Unit,
    onCancelOccurrence: () -> Unit,
    onDeleteSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val ready = (detail as? DetailState.Ready)?.detail
    val ownEntry = entry.agency == Agency.Mine

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Space.lg)) {
        Text(entry.title, style = SheetTitle, color = colors.ink)

        val meta = describe(entry)

        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            Text(meta, style = EntryMeta, color = colors.slate)
        }

        entry.description?.let { text ->
            Spacer(Modifier.height(Space.sm))
            Text(text, style = EntryTitle, color = colors.ink)
        }

        val states = occurrenceStates(entry)

        if (states.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))

            states.forEach { line ->
                Text(line, style = EntryMeta, color = colors.slate)
            }
        }

        if (ownEntry) {
            Spacer(Modifier.height(Space.md))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill("editar", onEdit, colors.ink)

                if (entry.isTask) {
                    Pill(if (entry.completed) "reabrir" else "concluir", onToggleCompletion, colors.brand)
                }

                if (entry.recurring) {
                    Pill("remover", onCancelOccurrence, colors.destructive)
                    Pill("remover série", onDeleteSeries, colors.destructive)
                } else {
                    Pill("remover", onDeleteSeries, colors.destructive)
                }
            }
        }

        val hasBlocks = ready != null && !ready.isEmpty

        if (detail is DetailState.Loading || hasBlocks) {
            Spacer(Modifier.height(Space.lg))

            if (detail is DetailState.Loading) Progress() else Hairline()
        }

        if (ready != null) Blocks(entry = entry, detail = ready)

        Spacer(Modifier.height(Space.md))
    }
}

@Composable
private fun Blocks(entry: CalendarEntry, detail: EntryDetail) {
    val colors = LocalChronicle.current

    if (detail.next.isNotEmpty()) {
        Section("próximas")

        detail.next.forEach { occurrence ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(Stroke.node).clip(CircleShape).background(entry.color))
                Spacer(Modifier.width(Space.sm))
                Text(occurrence.label, style = EntryTitle, color = colors.ink)
                Spacer(Modifier.weight(1f))
                Text(occurrence.relative, style = EntryMeta, color = colors.slate)
            }
        }

        detail.repeatsUntil?.let { until ->
            Text("repete até ${shortDate(until)}", style = EntryMeta, color = colors.slate)
        }
    }

    if (detail.reminders.isNotEmpty()) {
        Section("lembrete")

        detail.reminders.forEach { label ->
            Text(label, style = EntryTitle, color = colors.ink)
        }
    }

    if (detail.items.isNotEmpty()) {
        Section("checklist")

        detail.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("· ${item.title}", style = EntryTitle, color = colors.ink)

                item.durationMinutes?.let { minutes ->
                    Spacer(Modifier.weight(1f))
                    Text("$minutes min", style = EntryMeta, color = colors.slate)
                }
            }
        }
    }
}

@Composable
private fun Section(label: String) {
    val colors = LocalChronicle.current

    Spacer(Modifier.height(Space.lg))
    Text(label.uppercase(), style = Eyebrow, color = colors.slate)
    Spacer(Modifier.height(Space.sm))
}

@Composable
private fun Hairline() {
    val colors = LocalChronicle.current

    Spacer(
        Modifier
            .fillMaxWidth()
            .height(Stroke.hairline)
            .drawBehind { drawLine(colors.hairline, Offset(0f, 0f), Offset(size.width, 0f), size.height) }
    )
}

@Composable
private fun Progress() {
    val colors = LocalChronicle.current

    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().height(Stroke.hairline),
        color = colors.brand,
        trackColor = colors.hairline,
    )
}

@Composable
private fun describe(entry: CalendarEntry): AnnotatedString {
    val colors = LocalChronicle.current

    val plain = buildList {
        clockRange(entry)?.let { add(it) }
        if (entry.allDay && entry.agency == Agency.Mine) add("dia inteiro")
        entry.categoryName?.let { add(it) }
        if (entry.recurring) add(entry.recurrenceRule?.let { Recurrence.parse(it).summary() } ?: "repete")
    }

    val priority = priorityLabels[entry.priority]

    return buildAnnotatedString {
        plain.forEachIndexed { index, part ->
            if (index > 0) append(SEPARATOR)
            append(part)
        }

        if (priority != null) {
            if (plain.isNotEmpty()) append(SEPARATOR)

            if (entry.priority == "high") {
                withStyle(SpanStyle(color = colors.brand)) { append(priority) }
            } else {
                append(priority)
            }
        }

        entry.note?.let { note ->
            if (plain.isNotEmpty() || priority != null) append(SEPARATOR)
            append(note)
        }
    }
}

private const val SEPARATOR = " · "

private fun clockRange(entry: CalendarEntry): String? {
    val start = entry.clock ?: return null
    val end = entry.endTime

    return if (end == null || end == start) start else "$start – $end"
}

private fun occurrenceStates(entry: CalendarEntry): List<String> = buildList {
    if (entry.completed) {
        val on = entry.completedOn?.let { runCatching { shortDate(LocalDate.parse(it)) }.getOrNull() }

        add(if (on == null) "concluído" else "concluído em $on")
    }

    if (entry.overridden) add("esta ocorrência foi editada")
    if (entry.remindersMuted) add("lembretes silenciados nesta ocorrência")
}
