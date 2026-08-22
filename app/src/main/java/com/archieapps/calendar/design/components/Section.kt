package com.archieapps.calendar.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke

@Composable
fun Section(label: String, top: Dp = 0.dp) {
    val colors = LocalChronicle.current

    Column {
        if (top > 0.dp) {
            Spacer(Modifier.height(top))
        }

        Text(
            text = label.uppercase(),
            style = Eyebrow,
            color = colors.slate,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val colors = LocalChronicle.current

    Spacer(
        modifier
            .fillMaxWidth()
            .height(Stroke.hairline)
            .drawBehind {
                drawLine(
                    color = colors.hairline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = size.height,
                )
            }
    )
}
