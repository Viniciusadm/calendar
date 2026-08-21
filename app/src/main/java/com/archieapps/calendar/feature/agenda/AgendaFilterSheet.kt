package com.archieapps.calendar.feature.agenda

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.colorFromValue
import com.archieapps.calendar.design.components.DateStepper
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.TextAction
import java.time.LocalDate

@Composable
fun AgendaFilterSheet(
    filters: AgendaFilters,
    categories: List<CategoryDto>,
    onFrom: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onPickMonth: () -> Unit,
    onToggleCategory: (Int) -> Unit,
    onToggleKind: (String) -> Unit,
    onToggleNature: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("filtros", style = SheetTitle, color = colors.ink)

            Spacer(Modifier.weight(1f))

            if (filters.activeCount > 0) {
                TextAction("limpar", onClear, colors.brand, style = Eyebrow, horizontal = Space.sm)
            }
        }

        Section("a partir de")

        DateStepper(date = filters.from, onPick = onFrom, months = true)

        Spacer(Modifier.height(Space.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction("escolher mês", onPickMonth, colors.brand, style = Eyebrow, horizontal = Space.sm)

            if (!filters.anchoredToday) {
                TextAction("hoje", onToday, colors.brand, style = Eyebrow, horizontal = Space.sm)
            }
        }

        if (categories.isNotEmpty()) {
            Section("categorias")

            ScrollingPills {
                categories.forEach { category ->
                    Pill(
                        label = category.name,
                        selected = filters.categoryIds.contains(category.id),
                        onClick = { onToggleCategory(category.id) },
                        dot = colorFromValue(category.color),
                    )
                }
            }
        }

        Section("tipos")

        ScrollingPills {
            agendaKinds.forEach { (value, label) ->
                Pill(
                    label = label,
                    selected = filters.kinds.contains(value),
                    onClick = { onToggleKind(value) },
                )
            }
        }

        Section("natureza")

        ScrollingPills {
            agendaNatures.forEach { (value, label) ->
                Pill(
                    label = label,
                    selected = filters.natures.contains(value),
                    onClick = { onToggleNature(value) },
                )
            }
        }

        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun Section(label: String) {
    val colors = LocalChronicle.current

    Spacer(Modifier.height(Space.xl))
    Text(label.uppercase(), style = Eyebrow, color = colors.slate)
    Spacer(Modifier.height(Space.sm))
}
