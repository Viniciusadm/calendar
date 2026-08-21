package com.archieapps.calendar.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke

@Composable
fun HairlineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    textStyle: TextStyle = Eyebrow,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val selection = TextSelectionColors(
        handleColor = colors.brand,
        backgroundColor = colors.brand.copy(alpha = 0.25f),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))

        CompositionLocalProvider(LocalTextSelectionColors provides selection) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.brand),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = colors.hairline,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = Stroke.hairline.toPx(),
                        )
                    },
                decorationBox = { inner ->
                    Box(modifier = Modifier.padding(bottom = Space.sm)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = textStyle, color = colors.slate.copy(alpha = 0.55f))
                        }
                        inner()
                    }
                },
            )
        }
    }
}
