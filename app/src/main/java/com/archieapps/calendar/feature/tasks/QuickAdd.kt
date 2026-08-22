package com.archieapps.calendar.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.Glyph
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.TextAction
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus

private val rowHeight = 56.dp

@Composable
fun QuickAdd(
    draft: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onDetailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    if (!expanded) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = rowHeight)
                .clickable(onClick = onExpand)
                .semantics { contentDescription = "Nova tarefa" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(icon = Lucide.Plus, tint = colors.brand, size = 20.dp)

            Spacer(Modifier.width(Space.md))

            Text("nova tarefa", style = EntryTitle, color = colors.slate)
        }

        return
    }

    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(modifier = modifier.fillMaxWidth().padding(top = Space.md)) {
        HairlineField(
            value = draft,
            onValueChange = onDraft,
            label = "",
            placeholder = "o que precisa ser feito",
            textStyle = EntryTitle,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            focusRequester = focus,
        )

        Spacer(Modifier.height(Space.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction(
                label = "adicionar para hoje",
                onClick = onSubmit,
                color = if (draft.isBlank()) colors.slate else colors.brand,
                style = Eyebrow,
                enabled = draft.isNotBlank(),
            )

            Spacer(Modifier.weight(1f))

            TextAction("mais opções", onDetailed, colors.slate, style = Eyebrow)
        }
    }
}
