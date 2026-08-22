package com.archieapps.calendar.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.colorFromValue
import com.archieapps.calendar.design.components.CircleButton
import com.archieapps.calendar.design.components.Hairline
import com.archieapps.calendar.design.components.TextAction

@Composable
fun CategoriesScreen(
    state: CategoriesState,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (CategoryDto) -> Unit,
    onToggleArchive: (CategoryDto) -> Unit,
    onDelete: (CategoryDto) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.xl))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(glyph = "‹", label = "Voltar", onClick = onBack)

            Spacer(Modifier.width(Space.md))

            Text("categorias", style = MonthTitle.copy(fontSize = 26.sp), color = colors.ink)

            Spacer(Modifier.weight(1f))

            TextAction("nova", onNew, colors.brand, style = Eyebrow, horizontal = Space.sm)
        }

        Spacer(Modifier.height(Space.lg))

        if (state.loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(Stroke.hairline),
                color = colors.brand,
                trackColor = colors.hairline,
            )
        } else {
            Hairline()
        }

        state.error?.let { message ->
            Spacer(Modifier.height(Space.lg))
            Text(message, style = EntryMeta, color = colors.ink)
            TextAction("Tentar de novo", onRetry, colors.brand, style = Eyebrow)
        }

        Spacer(Modifier.height(Space.sm))

        if (state.items.isEmpty() && !state.loading) {
            Text("Nenhuma categoria.", style = EntryTitle, color = colors.slate)
        }

        state.items.forEachIndexed { index, category ->
            CategoryRow(
                category = category,
                onEdit = { onEdit(category) },
                onToggleArchive = { onToggleArchive(category) },
                onDelete = { onDelete(category) },
                onUp = { onMove(index, -1) },
                onDown = { onMove(index, 1) },
                canGoUp = index > 0,
                canGoDown = index < state.items.lastIndex,
            )

            if (index < state.items.lastIndex) Hairline()
        }

        Spacer(Modifier.height(Space.lg))

        Text(
            "A ordem aqui é a ordem das categorias no editor de evento. Categoria arquivada não aparece lá.",
            style = EntryMeta,
            color = colors.slate,
        )

        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun CategoryRow(
    category: CategoryDto,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    canGoUp: Boolean,
    canGoDown: Boolean,
) {
    val colors = LocalChronicle.current
    val meta = category.metaLine()
    val fade = if (category.active) 1f else 0.45f

    Column(Modifier.fillMaxWidth().padding(top = Space.md)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(Stroke.node)
                    .clip(CircleShape)
                    .background((colorFromValue(category.color) ?: colors.brand).copy(alpha = fade))
            )

            Spacer(Modifier.width(Space.md))

            Column(Modifier.weight(1f)) {
                Text(
                    text = category.pillLabel(),
                    style = EntryTitle,
                    color = colors.ink.copy(alpha = fade),
                )

                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(meta, style = EntryMeta, color = colors.slate.copy(alpha = fade))
                }
            }

            Arrow(glyph = "↑", label = "Subir ${category.name}", enabled = canGoUp, onClick = onUp)
            Arrow(glyph = "↓", label = "Descer ${category.name}", enabled = canGoDown, onClick = onDown)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Action("editar", colors.brand, onEdit)

            if (!category.isDefault) {
                Action(if (category.active) "arquivar" else "reativar", colors.ink, onToggleArchive)
                Action("excluir", colors.brand, onDelete)
            }
        }
    }
}

@Composable
private fun Action(label: String, color: Color, onClick: () -> Unit) {
    TextAction(label, onClick, color, style = Eyebrow, horizontal = Space.sm)
}

@Composable
private fun Arrow(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalChronicle.current

    Text(
        text = glyph,
        style = SheetTitle.copy(fontSize = 18.sp, letterSpacing = 0.sp),
        color = if (enabled) colors.slate else colors.hairline,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = Space.sm, vertical = Space.sm),
    )
}

