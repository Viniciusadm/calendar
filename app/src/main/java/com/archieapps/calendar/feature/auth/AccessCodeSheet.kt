package com.archieapps.calendar.feature.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.core.net.AccessCode
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.TextAction

@Composable
fun AccessCodeSheet(
    submitting: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val pairs = remember { AccessCode.pairs() }
    var code by remember { mutableStateOf("") }

    val filled = code.length / 2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg),
    ) {
        Text("código de acesso", style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))
        Text("•".repeat(filled).ifEmpty { "————" }, style = SheetTitle, color = colors.ink)

        Spacer(Modifier.height(Space.lg))

        pairs.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                row.forEach { (first, second) ->
                    PairKey(
                        label = "$first ou $second",
                        enabled = code.length < AccessCode.LENGTH,
                        onClick = { code += "$first$second" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(Space.sm))
            Text(error, style = EntryMeta, color = colors.brand)
        }

        Spacer(Modifier.height(Space.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextAction("Apagar", { code = "" }, colors.slate)

            TextAction(
                label = if (submitting) "Validando…" else "Liberar",
                onClick = { onSubmit(code) },
                color = colors.brand,
                enabled = code.length == AccessCode.LENGTH && !submitting,
            )

            Spacer(Modifier.weight(1f))

            TextAction("Depois", onDismiss, colors.slate)
        }

        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun PairKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .border(1.dp, colors.hairline, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = ButtonLabel,
            color = if (enabled) colors.ink else colors.slate.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
    }
}
