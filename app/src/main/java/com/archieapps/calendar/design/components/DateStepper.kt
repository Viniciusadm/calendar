package com.archieapps.calendar.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ChevronsLeft
import com.composables.icons.lucide.ChevronsRight
import com.composables.icons.lucide.Lucide
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun DateStepper(
    date: LocalDate,
    onPick: (LocalDate) -> Unit,
    months: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, ptBr)
    val month = date.month.getDisplayName(JavaTextStyle.FULL, ptBr)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        if (months) {
            CircleButton(icon = Lucide.ChevronsLeft, label = "Um mês antes", onClick = { onPick(date.minusMonths(1)) }, diameter = 36)
        }

        CircleButton(icon = Lucide.ChevronLeft, label = "Um dia antes", onClick = { onPick(date.minusDays(1)) }, diameter = 36)

        Text(
            text = "$weekday, ${date.dayOfMonth} de $month",
            style = EntryMeta,
            color = colors.ink,
        )

        CircleButton(icon = Lucide.ChevronRight, label = "Um dia depois", onClick = { onPick(date.plusDays(1)) }, diameter = 36)

        if (months) {
            CircleButton(icon = Lucide.ChevronsRight, label = "Um mês depois", onClick = { onPick(date.plusMonths(1)) }, diameter = 36)
        }
    }
}
