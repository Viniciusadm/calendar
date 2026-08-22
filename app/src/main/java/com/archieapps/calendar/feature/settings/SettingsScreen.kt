package com.archieapps.calendar.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.Avatar
import com.archieapps.calendar.design.components.CircleButton
import com.archieapps.calendar.design.components.Group
import com.archieapps.calendar.design.components.NavRow
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.PillRow
import com.archieapps.calendar.design.components.SwitchRow
import com.archieapps.calendar.design.components.TextAction
import com.archieapps.calendar.design.components.ValueRow
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus

@Composable
fun SettingsScreen(
    name: String?,
    email: String?,
    initial: String,
    photo: ImageBitmap?,
    preferences: Preferences,
    unlocked: Boolean,
    exactAlarmsAllowed: Boolean,
    canRequestExactAlarms: Boolean,
    onPreferences: ((Preferences) -> Preferences) -> Unit,
    onAskCode: () -> Unit,
    onLock: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onCategories: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.xl))

        Text("ajustes", style = MonthTitle, color = colors.ink)

        Spacer(Modifier.height(Space.xl))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(initial = initial, label = "Sua foto", photo = photo, diameter = 52)

            Spacer(Modifier.width(Space.md))

            Column {
                Text(name ?: "Você", style = SheetTitle, color = colors.ink)

                email?.let {
                    Spacer(Modifier.height(Space.xxs))
                    Text(it, style = EntryMeta, color = colors.slate)
                }
            }
        }

        Group("aparência")

        ValueRow(label = "tema") {
            PillRow {
                themeChoices.forEach { (value, text) ->
                    Pill(
                        label = text,
                        selected = preferences.themeMode == value,
                        onClick = { onPreferences { it.copy(themeMode = value) } },
                    )
                }
            }
        }

        ValueRow(label = "primeiro dia") {
            PillRow {
                weekStartChoices.forEach { (value, text) ->
                    Pill(
                        label = text,
                        selected = preferences.weekStartValue == value,
                        onClick = { onPreferences { it.copy(weekStartsMonday = value == "monday") } },
                    )
                }
            }
        }

        Group("tarefas")

        ValueRow(label = "abrir em") {
            PillRow {
                initialFilterChoices.forEach { (value, text) ->
                    Pill(
                        label = text,
                        selected = preferences.initialTaskFilter == value,
                        onClick = { onPreferences { it.copy(initialTaskFilter = value) } },
                    )
                }
            }
        }

        SwitchRow(
            label = "rotina em todos os dias",
            caption = "senão, só no dia de hoje e no que você tocar",
            checked = preferences.expandRecurringInGrid,
            onToggle = { value -> onPreferences { it.copy(expandRecurringInGrid = value) } },
        )

        Group("conteúdo")

        NavRow(label = "categorias", caption = "cor, ícone e prioridade padrão", onClick = onCategories)

        Group("notificações")

        SwitchRow(
            label = "resumo diário",
            caption = "uma notificação com as tarefas do dia",
            checked = preferences.digestEnabled,
            onToggle = { value -> onPreferences { it.copy(digestEnabled = value) } },
        )

        if (preferences.digestEnabled) {
            ValueRow(label = "horário") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    CircleButton(
                        icon = Lucide.Minus,
                        label = "Meia hora antes",
                        onClick = { onPreferences { it.copy(digestMinuteOfDay = previousDigestSlot(it.digestMinuteOfDay)) } },
                        diameter = 34,
                    )

                    Text(
                        text = preferences.digestLabel,
                        style = EntryTitle,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = Space.huge),
                    )

                    CircleButton(
                        icon = Lucide.Plus,
                        label = "Meia hora depois",
                        onClick = { onPreferences { it.copy(digestMinuteOfDay = nextDigestSlot(it.digestMinuteOfDay)) } },
                        diameter = 34,
                    )
                }
            }

            SwitchRow(
                label = "avisar tarefa atrasada",
                checked = preferences.notifyOverdue,
                onToggle = { value -> onPreferences { it.copy(notifyOverdue = value) } },
            )
        }

        if (!exactAlarmsAllowed && canRequestExactAlarms) {
            NavRow(
                label = "permitir alarme exato",
                caption = "o Android está atrasando os lembretes",
                onClick = onRequestExactAlarms,
            )
        }

        Group("privacidade")

        if (unlocked) {
            NavRow(label = "travar de novo", caption = "esconde o que exige código", onClick = onLock)
        } else {
            NavRow(label = "digitar o código", caption = "mostra o que está protegido", onClick = onAskCode)
        }

        Spacer(Modifier.height(Space.xxl))

        TextAction("Sair da conta", onSignOut, colors.slate, stretch = true, align = Alignment.Center)

        Spacer(Modifier.height(Space.huge))
    }
}
