package com.archieapps.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.alarm.ExactAlarms
import com.archieapps.calendar.core.media.AvatarLoader
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.sync.ReminderSync
import com.archieapps.calendar.design.CalendarTheme
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.feature.auth.AccountSheet
import com.archieapps.calendar.feature.auth.LoginScreen
import com.archieapps.calendar.feature.auth.LoginViewModel
import com.archieapps.calendar.feature.calendar.CalendarViewModel
import com.archieapps.calendar.feature.calendar.EntrySheet
import com.archieapps.calendar.feature.calendar.EventEditor
import com.archieapps.calendar.feature.calendar.MonthScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                Root()
            }
        }
    }
}

@Composable
private fun Root() {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val colors = LocalChronicle.current
    var loggedIn by remember { mutableStateOf(settings.isLoggedIn) }

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = colors.ground) { insets ->
        Box(Modifier.padding(insets)) {
            if (loggedIn) {
                Chronicle(
                    settings = settings,
                    onSignedOut = {
                        settings.clearSession()
                        loggedIn = false
                    },
                )
            } else {
                Login(settings = settings, onAuthenticated = { loggedIn = true })
            }
        }
    }
}

@Composable
private fun Login(settings: Settings, onAuthenticated: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(
        key = "login",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LoginViewModel(CalendarApi(settings), settings, onAuthenticated) as T
        }
    )

    val state by viewModel.state.collectAsState()

    LoginScreen(
        state = state,
        onEmail = viewModel::onEmail,
        onPassword = viewModel::onPassword,
        onSubmit = viewModel::submit,
    )
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
private fun Chronicle(settings: Settings, onSignedOut: () -> Unit) {
    val colors = LocalChronicle.current
    val context = LocalContext.current
    val viewModel: CalendarViewModel = viewModel(
        key = "calendar",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(CalendarApi(settings)) as T
        }
    )

    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var accountOpen by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(Triple(settings.userName, settings.userEmail, settings.userImage)) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        when (val result = CalendarApi(settings).profile()) {
            is ApiResult.Ok -> {
                result.value.name?.let { settings.userName = it }
                result.value.email?.let { settings.userEmail = it }
                result.value.image?.let { settings.userImage = it }
                profile = Triple(settings.userName, settings.userEmail, settings.userImage)
            }

            is ApiResult.Failure -> Unit
        }
    }

    LaunchedEffect(profile.third) {
        photo = AvatarLoader.load(context, profile.third)
    }

    NotificationPermissionGate()

    LaunchedEffect(state.unauthorized) {
        if (state.unauthorized) onSignedOut()
    }

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
                contentColor = if (colors.isDark) colors.ground else Color.White,
            ) {
                Text("+", style = Eyebrow)
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets)) {
            MonthScreen(
                state = state,
                onSelect = viewModel::select,
                onShowMonth = viewModel::showMonth,
                onToday = viewModel::today,
                onRetry = viewModel::reload,
                onOpenEntry = viewModel::focus,
                onToggleEntry = viewModel::toggleCompletion,
                onAccount = { accountOpen = true },
                accountInitial = accountInitial(profile.first, profile.second),
                accountPhoto = photo,
            )
        }
    }

    if (accountOpen) {
        ModalBottomSheet(
            onDismissRequest = { accountOpen = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface,
        ) {
            AccountSheet(
                name = profile.first,
                email = profile.second,
                photo = photo,
                initial = accountInitial(profile.first, profile.second),
                privateUnlocked = state.privateUnlocked,
                unlocking = state.unlocking,
                unlockError = state.unlockError,
                onUnlock = viewModel::unlockPrivate,
                onLock = viewModel::lockPrivate,
                exactAlarmsAllowed = ExactAlarms.allowed(context),
                canRequestExactAlarms = ExactAlarms.requestable(),
                onRequestExactAlarms = { ExactAlarms.request(context) },
                onSignOut = {
                    accountOpen = false
                    onSignedOut()
                },
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

private fun accountInitial(name: String?, email: String?): String {
    val source = name?.trim()?.takeIf { it.isNotEmpty() } ?: email?.trim()

    return source?.firstOrNull()?.uppercase() ?: "?"
}
