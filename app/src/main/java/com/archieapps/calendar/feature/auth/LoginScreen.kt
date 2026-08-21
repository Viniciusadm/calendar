package com.archieapps.calendar.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.ButtonLabel
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.HairlineField

@Composable
fun LoginScreen(
    state: LoginState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Space.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("sua agenda", style = Eyebrow, color = colors.slate)
        Spacer(Modifier.height(Space.sm))
        Text("entrar", style = MonthTitle, color = colors.ink)

        Spacer(Modifier.height(Space.xxl))

        HairlineField(
            value = state.email,
            onValueChange = onEmail,
            label = "e-mail",
            placeholder = "voce@exemplo.com",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )

        Spacer(Modifier.height(Space.xl))

        HairlineField(
            value = state.password,
            onValueChange = onPassword,
            label = "senha",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            visualTransformation = PasswordVisualTransformation(),
            onImeAction = onSubmit,
        )

        if (state.error != null) {
            Spacer(Modifier.height(Space.lg))
            Text(state.error, style = EntryMeta, color = colors.brand)
        }

        Spacer(Modifier.height(Space.xxl))

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.brand),
        ) {
            Text(if (state.loading) "Entrando…" else "Entrar", style = ButtonLabel)
        }
    }
}
