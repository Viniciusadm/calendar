package com.archieapps.calendar.feature.categories

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.colorFromValue
import com.archieapps.calendar.design.components.Pill

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

    Column(modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Space.lg)) {
        Text("excluir ${category.name}", style = SheetTitle, color = colors.ink)

        Spacer(Modifier.height(Space.sm))

        Text(
            text = summary?.let { "Em uso por $it." } ?: "Nada está usando essa categoria.",
            style = EntryMeta,
            color = colors.slate,
        )

        Spacer(Modifier.height(Space.xl))

        if (summary != null && targets.isNotEmpty()) {
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

            Spacer(Modifier.height(Space.xl))
        }

        Action(
            label = if (summary == null) "Excluir" else "Excluir e deixar sem categoria",
            color = colors.brand,
            onClick = onForce,
        )

        Action("Cancelar", colors.slate, onDismiss)

        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
private fun Action(label: String, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = ButtonLabel, color = color, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
    }
}
