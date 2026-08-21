package com.archieapps.calendar.feature.categories

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.colorFromValue
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.TextAction

@Composable
fun CategoryDeletePrompt(
    category: CategoryDto,
    targets: List<CategoryDto>,
    onReassign: (Int) -> Unit,
    onForce: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val summary = category.linkSummary()

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Space.lg)) {
        Text("excluir ${category.name}", style = SheetTitle, color = colors.ink)

        Spacer(Modifier.height(Space.sm))

        Text(
            text = summary?.let { "Em uso por $it." } ?: "Nada está usando essa categoria.",
            style = EntryMeta,
            color = colors.slate,
        )

        if (summary != null && targets.isNotEmpty()) {
            Spacer(Modifier.height(Space.xl))
            Text("MOVER PARA", style = Eyebrow, color = colors.slate)
            Spacer(Modifier.height(Space.sm))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                targets.forEach { target ->
                    Pill(
                        label = target.pillLabel(),
                        selected = false,
                        onClick = { onReassign(target.id) },
                        dot = colorFromValue(target.color),
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))

        TextAction(
            label = if (summary == null) "Excluir" else "Excluir e deixar sem categoria",
            onClick = onForce,
            color = colors.brand,
            stretch = true,
        )

        TextAction("Cancelar", onDismiss, colors.slate, stretch = true)

        Spacer(Modifier.height(Space.sm))
    }
}
