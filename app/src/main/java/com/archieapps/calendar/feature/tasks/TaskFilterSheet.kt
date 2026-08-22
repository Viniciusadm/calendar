package com.archieapps.calendar.feature.tasks

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
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.Section
import com.archieapps.calendar.design.components.TextAction

val taskPriorities: List<Pair<String, String>> = listOf(
    "high" to "alta",
    "medium" to "média",
    "low" to "baixa",
    "none" to "nenhuma",
)

@Composable
fun TaskFilterSheet(
    state: TaskListState,
    categories: List<CategoryDto>,
    onToggleCategory: (Int) -> Unit,
    onTogglePriority: (String) -> Unit,
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

            if (state.activeFilterCount > 0) {
                TextAction("limpar", onClear, colors.brand, style = Eyebrow, horizontal = Space.sm)
            }
        }

        if (categories.isNotEmpty()) {
            Section("categorias", top = Space.xl)

            ScrollingPills {
                categories.forEach { category ->
                    Pill(
                        label = category.name,
                        selected = state.categoryIds.contains(category.id),
                        onClick = { onToggleCategory(category.id) },
                        dot = colorFromValue(category.color),
                    )
                }
            }
        }

        Section("prioridade", top = Space.xl)

        ScrollingPills {
            taskPriorities.forEach { (value, label) ->
                Pill(
                    label = label,
                    selected = state.priorities.contains(value),
                    onClick = { onTogglePriority(value) },
                )
            }
        }

        Spacer(Modifier.height(Space.xxl))
    }
}
