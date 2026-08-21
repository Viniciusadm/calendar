package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.MonthTitle
import com.archieapps.calendar.design.Space

@Composable
fun SetupScreen(
    initialBaseUrl: String,
    onSave: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var token by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.lg),
    ) {
        Spacer(Modifier.height(Space.huge))
        Text("conectar", style = MonthTitle, color = colors.ink)
        Spacer(Modifier.height(Space.sm))
        Text(
            "Informe o endereço do servidor e o token de acesso para carregar sua agenda.",
            style = EntryMeta,
            color = colors.slate,
        )

        Spacer(Modifier.height(Space.xl))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Servidor") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        Spacer(Modifier.height(Space.md))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        Spacer(Modifier.height(Space.xl))

        Button(
            onClick = { onSave(baseUrl.trim(), token.trim()) },
            enabled = baseUrl.isNotBlank() && token.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Carregar agenda", style = Eyebrow)
        }
    }
}
