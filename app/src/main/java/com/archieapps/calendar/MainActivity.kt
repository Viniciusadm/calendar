package com.archieapps.calendar

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.sync.ReminderSync
import com.archieapps.calendar.design.CalendarTheme
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.feature.calendar.CalendarViewModel
import com.archieapps.calendar.feature.calendar.EntrySheet
import com.archieapps.calendar.feature.calendar.EventEditor
import com.archieapps.calendar.feature.calendar.MonthScreen
import com.archieapps.calendar.feature.calendar.SetupScreen
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                CalendarApp()
            }
        }
    }
}

@Composable
private fun CalendarApp() {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var configured by remember { mutableStateOf(settings.isConfigured) }

    if (!configured) {
        val colors = LocalChronicle.current

        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = colors.ground) { insets ->
            Box(Modifier.padding(insets)) {
                SetupScreen(
                    initialBaseUrl = settings.baseUrl,
                    onSave = { baseUrl, token ->
                        settings.baseUrl = baseUrl
                        settings.token = token
                        configured = true
                    },
                )
            }
        }

        return
    }

    Chronicle(settings)
}

@Composable
private fun NotificationPermissionGate() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chronicle(settings: Settings) {
    val colors = LocalChronicle.current
    val viewModel: CalendarViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(CalendarApi(settings)) as T
        }
    )

    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    NotificationPermissionGate()

    LaunchedEffect(state.writeTick) {
        ReminderSync.run(context, force = state.writeTick > 0)
    }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissNotice()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.ground,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::newEvent,
                containerColor = colors.brand,
                contentColor = if (colors.isDark) colors.ground else androidx.compose.ui.graphics.Color.White,
            ) {
                Text("+", style = Eyebrow)
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets)) {
            MonthScreen(
                state = state,
                onSelect = viewModel::select,
                onShiftMonth = viewModel::goToMonth,
                onToday = viewModel::today,
                onRetry = viewModel::load,
                onOpenEntry = viewModel::focus,
                onToggleEntry = viewModel::toggleCompletion,
            )
        }
    }

    state.focused?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.focus(null) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface,
        ) {
            EntrySheet(
                entry = entry,
                onEdit = viewModel::editFocused,
                onToggleCompletion = {
                    viewModel.toggleCompletion(entry)
                    viewModel.focus(null)
                },
                onCancelOccurrence = viewModel::cancelFocusedOccurrence,
                onDeleteSeries = viewModel::deleteFocusedSeries,
                onDismiss = { viewModel.focus(null) },
            )
        }
    }

    state.draft?.let { draft ->
        ModalBottomSheet(
            onDismissRequest = viewModel::closeDraft,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            EventEditor(
                draft = draft,
                categories = state.categories,
                saving = state.saving,
                onChange = viewModel::updateDraft,
                onSave = viewModel::saveDraft,
                onCancel = viewModel::closeDraft,
            )
        }
    }
}
