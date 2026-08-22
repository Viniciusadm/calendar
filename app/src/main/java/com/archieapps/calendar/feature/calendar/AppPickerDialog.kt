package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.archieapps.calendar.core.action.InstalledApp
import com.archieapps.calendar.core.action.TaskActions
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.HairlineField
import com.archieapps.calendar.design.components.TextAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconSize = 34.dp

@Composable
fun AppPickerDialog(onPick: (InstalledApp) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalChronicle.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val apps by produceState(initialValue = emptyList<InstalledApp>()) {
        value = withContext(Dispatchers.IO) { TaskActions.installedApps(context) }
    }

    val shown = remember(apps, query) {
        val needle = query.trim()

        if (needle.isBlank()) apps else apps.filter { it.label.contains(needle, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Space.md))
                .background(colors.surface)
                .padding(horizontal = Space.lg, vertical = Space.xl),
        ) {
            Text("escolher aplicativo", style = SheetTitle, color = colors.ink)

            Spacer(Modifier.height(Space.lg))

            HairlineField(
                value = query,
                onValueChange = { query = it },
                label = "",
                placeholder = "buscar",
            )

            Spacer(Modifier.height(Space.md))

            if (apps.isEmpty()) {
                Text("carregando…", style = EntryMeta, color = colors.slate)
            } else if (shown.isEmpty()) {
                Text("nenhum aplicativo encontrado", style = EntryMeta, color = colors.slate)
            }

            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(shown, key = { it.packageName }) { app ->
                    AppRow(app = app, onClick = { onPick(app) })
                }
            }

            Spacer(Modifier.height(Space.md))

            TextAction(
                label = "Cancelar",
                onClick = onDismiss,
                color = colors.slate,
                stretch = true,
                align = Alignment.Center,
            )
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    val colors = LocalChronicle.current
    val context = LocalContext.current

    val painter = remember(app.packageName) {
        TaskActions.icon(context, app.packageName)
            ?.let { runCatching { BitmapPainter(it.toBitmap().asImageBitmap()) }.getOrNull() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            if (painter != null) {
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(iconSize),
                )
            }
        }

        Spacer(Modifier.width(Space.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = EntryTitle, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, style = EntryMeta, color = colors.slate, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
