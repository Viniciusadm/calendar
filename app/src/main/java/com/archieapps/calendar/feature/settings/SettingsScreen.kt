package com.archieapps.calendar.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.SheetTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.Avatar
import com.archieapps.calendar.design.components.ChoiceRow
import com.archieapps.calendar.design.components.Hairline
import com.archieapps.calendar.design.components.NavRow
import com.archieapps.calendar.design.components.Section
import com.archieapps.calendar.design.components.SwitchRow
import com.archieapps.calendar.design.components.TextAction

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
        Hairline()

        ChoiceRow(
            label = "tema",
            options = themeChoices,
            selected = preferences.themeMode,
            onSelect = { value -> onPreferences { it.copy(themeMode = value) } },
        )

        ChoiceRow(
            label = "semana começa em",
            options = weekStartChoices,
            selected = preferences.weekStartValue,
            onSelect = { value -> onPreferences { it.copy(weekStartsMonday = value == "monday") } },
        )

        Section("tarefas", top = Space.xl)

        ChoiceRow(
            label = "abrir em",
            options = initialFilterChoices,
            selected = preferences.initialTaskFilter,
            onSelect = { value -> onPreferences { it.copy(initialTaskFilter = value) } },
        )

        SwitchRow(
            label = "tarefas recorrentes na grade",
            caption = "sem isso, rotina aparece só no dia de hoje e no dia selecionado",
            checked = preferences.expandRecurringInGrid,
            onToggle = { value -> onPreferences { it.copy(expandRecurringInGrid = value) } },
        )

        Hairline()

        Section("conteúdo", top = Space.xl)

        NavRow(label = "categorias", onClick = onCategories)

        Hairline()

        Section("notificações", top = Space.xl)

        SwitchRow(
            label = "resumo diário",
            caption = "uma notificação com as tarefas do dia",
            checked = preferences.digestEnabled,
            onToggle = { value -> onPreferences { it.copy(digestEnabled = value) } },
        )

        if (preferences.digestEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("horário", style = Eyebrow, color = colors.slate, modifier = Modifier.weight(1f))

                TextAction(
                    label = "−",
                    onClick = { onPreferences { it.copy(digestMinuteOfDay = previousDigestSlot(it.digestMinuteOfDay)) } },
                    color = colors.slate,
                    horizontal = Space.md,
                )

                Text(preferences.digestLabel, style = SheetTitle, color = colors.ink)

                TextAction(
                    label = "+",
                    onClick = { onPreferences { it.copy(digestMinuteOfDay = nextDigestSlot(it.digestMinuteOfDay)) } },
                    color = colors.slate,
                    horizontal = Space.md,
                )
            }

            SwitchRow(
                label = "avisar tarefa atrasada",
                checked = preferences.notifyOverdue,
                onToggle = { value -> onPreferences { it.copy(notifyOverdue = value) } },
            )
        }

        if (!exactAlarmsAllowed && canRequestExactAlarms) {
            NavRow(label = "permitir alarme exato", onClick = onRequestExactAlarms)
        }

        Hairline()

        Section("privacidade", top = Space.xl)

        if (unlocked) {
            NavRow(label = "travar de novo", onClick = onLock)
        } else {
            NavRow(label = "digitar o código", onClick = onAskCode)
        }

        Spacer(Modifier.height(Space.xl))

        TextAction("Sair da conta", onSignOut, colors.slate, stretch = true, align = Alignment.Center)

        Spacer(Modifier.height(Space.huge))
    }
}
