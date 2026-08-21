package com.archieapps.calendar.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.Avatar
import com.archieapps.calendar.design.components.TextAction

@Composable
fun AccountSheet(
    name: String?,
    email: String?,
    photo: ImageBitmap?,
    initial: String,
    unlocked: Boolean,
    onAskCode: () -> Unit,
    onLock: () -> Unit,
    exactAlarmsAllowed: Boolean,
    canRequestExactAlarms: Boolean,
    onRequestExactAlarms: () -> Unit,
    onCategories: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Space.lg)) {
        Text("conta", style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(initial = initial, label = "Sua foto", photo = photo, diameter = 56)

            Spacer(Modifier.width(Space.md))

            Column {
                Text(name ?: "Você", style = SheetTitle, color = colors.ink)

                if (email != null) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(email, style = EntryMeta, color = colors.slate)
                }
            }
        }

        Spacer(Modifier.height(Space.xl))

        PrivateSection(unlocked = unlocked, onAskCode = onAskCode, onLock = onLock)

        Spacer(Modifier.height(Space.md))

        Text("calendário", style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.xs))

        TextAction("Categorias", onCategories, colors.brand)

        if (! exactAlarmsAllowed && canRequestExactAlarms) {
            TextAction("Permitir alarme exato", onRequestExactAlarms, colors.brand)
        }

        Spacer(Modifier.height(Space.md))

        TextAction("Sair da conta", onSignOut, colors.slate, stretch = true)

        Spacer(Modifier.height(Space.sm))
    }
}

@Composable
private fun PrivateSection(unlocked: Boolean, onAskCode: () -> Unit, onLock: () -> Unit) {
    val colors = LocalChronicle.current

    Text("acesso", style = Eyebrow, color = colors.slate)
    Spacer(Modifier.height(Space.xs))

    if (unlocked) {
        TextAction("Travar de novo", onLock, colors.brand)

        return
    }

    TextAction("Digitar o código", onAskCode, colors.brand)
}
