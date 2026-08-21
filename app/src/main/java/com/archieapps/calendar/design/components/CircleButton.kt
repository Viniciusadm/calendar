package com.archieapps.calendar.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle

@Composable
fun CircleButton(
    glyph: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 40,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = SheetTitle.copy(fontSize = 20.sp, letterSpacing = 0.sp), color = colors.slate)
    }
}

@Composable
fun SearchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 36,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier.size((diameter / 2).dp).drawBehind {
                val stroke = 1.5.dp.toPx()
                val radius = size.minDimension * 0.34f
                val center = Offset(size.width * 0.42f, size.height * 0.42f)

                drawCircle(
                    color = colors.slate,
                    radius = radius,
                    center = center,
                    style = DrawStroke(width = stroke),
                )

                val edge = radius * 0.72f

                drawLine(
                    color = colors.slate,
                    start = Offset(center.x + edge, center.y + edge),
                    end = Offset(size.width * 0.96f, size.height * 0.96f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        )
    }
}

@Composable
fun Avatar(
    initial: String,
    label: String,
    photo: ImageBitmap?,
    modifier: Modifier = Modifier,
    diameter: Int = 36,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalChronicle.current

    Box(
        modifier = modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .border(1.dp, colors.hairline, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(initial, style = Eyebrow, color = colors.slate)
        }
    }
}
