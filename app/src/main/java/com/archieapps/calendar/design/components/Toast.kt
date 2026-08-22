package com.archieapps.calendar.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import kotlinx.coroutines.withTimeoutOrNull

private const val toastMillis = 3_000L

private val toastShape = RoundedCornerShape(Space.md)

suspend fun SnackbarHostState.showBriefly(
    message: String,
    actionLabel: String? = null,
): SnackbarResult? = withTimeoutOrNull(toastMillis) {
    showSnackbar(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = false,
        duration = SnackbarDuration.Indefinite,
    )
}

@Composable
fun ChronicleToastHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = LocalChronicle.current

    SnackbarHost(hostState = state, modifier = modifier) { data ->
        val swipe = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                val leaving = value != SwipeToDismissBoxValue.Settled

                if (leaving) {
                    data.dismiss()
                }

                leaving
            },
        )

        SwipeToDismissBox(
            state = swipe,
            backgroundContent = { Box(Modifier.fillMaxSize()) },
        ) {
            Snackbar(
                snackbarData = data,
                shape = toastShape,
                containerColor = colors.surface,
                contentColor = colors.ink,
                actionColor = colors.brand,
            )
        }
    }
}
