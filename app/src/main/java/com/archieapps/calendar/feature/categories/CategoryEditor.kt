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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.paletteTokens
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.TextAction

@Composable
fun CategoryEditor(
    draft: CategoryDraft,
    category: CategoryDto?,
    saving: Boolean,
    onChange: ((CategoryDraft) -> CategoryDraft) -> Unit,
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
            text = if (draft.isEditing) "editar" else "nova",
            style = MonthTitle,
            color = colors.ink,
        )

        Spacer(Modifier.height(Space.xl))

        HairlineField(
            value = draft.name,
            onValueChange = { value -> onChange { it.copy(name = value.take(60)) } },
            label = "nome",
            textStyle = SheetTitle,
        )

        Spacer(Modifier.height(Space.lg))

        HairlineField(
            value = draft.emoji,
            onValueChange = { value -> onChange { it.withEmoji(value) } },
            label = "emoji",
            placeholder = "🏃",
            textStyle = SheetTitle,
        )

        Spacer(Modifier.height(Space.xl))
        Section("cor")
        ScrollingPills {
            paletteTokens.forEach { token ->
                Pill(
                    label = token.label,
                    selected = draft.color == token.token,
                    onClick = { onChange { it.withToken(token.token) } },
                    dot = token.color,
                )
            }
        }

        Spacer(Modifier.height(Space.md))

        HairlineField(
            value = if (draft.isCustomColor) draft.color else "",
            onValueChange = { value -> onChange { it.withHex(value) } },
            label = "ou um hex",
            placeholder = "#349EF4",
            keyboardType = KeyboardType.Ascii,
        )

        Spacer(Modifier.height(Space.xl))
        Section("prioridade padrão")
        ScrollingPills {
            priorityChoices.forEach { (value, label) ->
                Pill(
                    label = label,
                    selected = draft.defaultPriority == value,
                    onClick = { onChange { it.copy(defaultPriority = value) } },
                    dot = null,
                )
            }
        }

        Spacer(Modifier.height(Space.sm))

        Text(
            "Vale para evento criado nesta categoria sem prioridade escolhida.",
            style = EntryMeta,
            color = colors.slate,
        )

        Spacer(Modifier.height(Space.xxl))

        Button(
            onClick = onSave,
            enabled = draft.canSave && !saving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand),
        ) {
            Text(if (saving) "Salvando…" else "Salvar", style = ButtonLabel)
        }

        Spacer(Modifier.height(Space.sm))

        TextAction(
            label = "Cancelar",
            onClick = onCancel,
            color = colors.slate,
            stretch = true,
            align = Alignment.Center,
        )

        if (category != null && category.isDefault) {
            Spacer(Modifier.height(Space.lg))
            Text(
                "É a categoria padrão do calendário: não pode ser arquivada nem excluída.",
                style = EntryMeta,
                color = colors.slate,
            )
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
