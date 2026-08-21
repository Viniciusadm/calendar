package com.archieapps.calendar.feature.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import com.archieapps.calendar.design.components.Avatar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space

@Composable
fun AccountSheet(
    name: String?,
    email: String?,
    photo: ImageBitmap?,
    initial: String,
    exactAlarmsAllowed: Boolean,
    canRequestExactAlarms: Boolean,
    onRequestExactAlarms: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Space.lg),
    ) {
        Text("conta", style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.md))

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

        Text("lembretes", style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))

        if (exactAlarmsAllowed) {
            Text("Os avisos tocam na hora marcada.", style = EntryMeta, color = colors.slate)
        } else {
            Text(
                "O sistema não está deixando este app usar alarme exato, então os avisos podem atrasar alguns minutos.",
                style = EntryMeta,
                color = colors.slate,
            )

            if (canRequestExactAlarms) {
                TextButton(onClick = onRequestExactAlarms, contentPadding = PaddingValues(0.dp)) {
                    Text("Permitir alarme exato", style = ButtonLabel, color = colors.brand)
                }
            }
        }

        Spacer(Modifier.height(Space.xl))

        TextButton(
            onClick = onSignOut,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Sair da conta",
                style = ButtonLabel,
                color = colors.slate,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(Space.lg))
    }
}
