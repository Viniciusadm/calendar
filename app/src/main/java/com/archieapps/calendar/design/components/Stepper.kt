package com.archieapps.calendar.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke

@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    unit: String,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val numeral = SheetTitle.copy(fontSize = 20.sp, letterSpacing = 0.sp, textAlign = TextAlign.Center)

    var typed by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    fun commit() {
        typed?.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) }
        typed = null
    }

    fun step(delta: Int) {
        val base = typed?.toIntOrNull()?.coerceIn(min, max) ?: value
        typed = null
        onChange((base + delta).coerceIn(min, max))
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        CircleButton(
            glyph = "−",
            label = "Diminuir",
            onClick = { step(-1) },
            diameter = 36,
        )

        Box(
            modifier = Modifier
                .widthIn(min = 44.dp)
                .drawBehind {
                    drawLine(
                        color = colors.hairline,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = Stroke.hairline.toPx(),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val draft = typed

            if (draft == null) {
                Text(
                    text = value.toString(),
                    style = numeral,
                    color = colors.ink,
                    modifier = Modifier
                        .clickable { typed = value.toString() }
                        .padding(bottom = Space.xs),
                )
            } else {
                var touched by remember { mutableStateOf(false) }

                BasicTextField(
                    value = draft,
                    onValueChange = { input -> typed = input.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    textStyle = numeral.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier
                        .focusRequester(focus)
                        .onFocusChanged { state ->
                            if (state.isFocused) touched = true else if (touched) commit()
                        }
                        .padding(bottom = Space.xs),
                )

                LaunchedEffect(Unit) { focus.requestFocus() }
            }
        }

        CircleButton(
            glyph = "+",
            label = "Aumentar",
            onClick = { step(1) },
            diameter = 36,
        )

        Text(unit, style = EntryMeta, color = colors.slate)
    }
}
