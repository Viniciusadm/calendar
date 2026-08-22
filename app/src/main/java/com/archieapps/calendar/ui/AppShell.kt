package com.archieapps.calendar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.calendar.core.alarm.DigestScheduler
import com.archieapps.calendar.core.alarm.ExactAlarms
import com.archieapps.calendar.core.media.AvatarLoader
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.sync.ReminderSync
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.components.ActionButton
import com.archieapps.calendar.design.components.ChronicleToastHost
import com.archieapps.calendar.design.components.TabBar
import com.archieapps.calendar.design.components.showBriefly
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.archieapps.calendar.feature.agenda.AgendaHost
import com.archieapps.calendar.feature.agenda.AgendaViewModel
import com.archieapps.calendar.feature.auth.AccessCodeSheet
import com.archieapps.calendar.feature.auth.LoginScreen
import com.archieapps.calendar.feature.auth.LoginViewModel
import com.archieapps.calendar.feature.calendar.CalendarViewModel
import com.archieapps.calendar.feature.calendar.EntrySheet
import com.archieapps.calendar.feature.calendar.EventEditor
import com.archieapps.calendar.feature.calendar.MonthPickerSheet
import com.archieapps.calendar.feature.calendar.MonthScreen
import com.archieapps.calendar.feature.categories.CategoriesHost
import com.archieapps.calendar.feature.settings.Preferences
import com.archieapps.calendar.feature.settings.SettingsScreen
import com.archieapps.calendar.feature.tasks.TaskFilter
import com.archieapps.calendar.feature.tasks.TasksHost
import com.archieapps.calendar.core.store.WidgetRevision
import com.archieapps.calendar.feature.widget.WidgetBridge
import com.archieapps.calendar.feature.tasks.TasksViewModel

@Composable
fun Root(
    settings: Settings,
    preferences: Preferences,
    onPreferences: ((Preferences) -> Preferences) -> Unit,
    focusOccurrence: String? = null,
    onFocusHandled: () -> Unit = {},
) {
    val colors = LocalChronicle.current
    var loggedIn by remember { mutableStateOf(settings.isLoggedIn) }

    Box(Modifier.fillMaxSize().background(colors.ground)) {
        if (loggedIn) {
            Shell(
                settings = settings,
                preferences = preferences,
                onPreferences = onPreferences,
                focusOccurrence = focusOccurrence,
                onFocusHandled = onFocusHandled,
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
private fun Shell(
    settings: Settings,
    preferences: Preferences,
    onPreferences: ((Preferences) -> Preferences) -> Unit,
    focusOccurrence: String?,
    onFocusHandled: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val colors = LocalChronicle.current
    val context = LocalContext.current
    val api = remember(settings) { CalendarApi(settings) }

    val viewModel: CalendarViewModel = viewModel(
        key = "calendar",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(api) as T
        }
    )

    val agendaViewModel: AgendaViewModel = viewModel(
        key = "agenda",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AgendaViewModel(api) as T
        }
    )

    val tasksViewModel: TasksViewModel = viewModel(
        key = "tasks",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TasksViewModel(api, TaskFilter.of(settings.initialTaskFilter)) as T
        }
    )

    val state by viewModel.state.collectAsState()
    val tasksState by tasksViewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(Tab.Tasks) }
    var leaf by remember { mutableStateOf(Leaf.Root) }
    var pickerOpen by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(Triple(settings.userName, settings.userEmail, settings.userImage)) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        when (val result = api.profile()) {
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

    LaunchedEffect(state.unauthorized, tasksState.unauthorized) {
        if (state.unauthorized || tasksState.unauthorized) onSignedOut()
    }

    val widgetWrites by WidgetRevision.fromWidget.collectAsState()

    LaunchedEffect(widgetWrites) {
        if (widgetWrites > 0) {
            tasksViewModel.markStale()
            tasksViewModel.reloadIfStale()
        }
    }

    LaunchedEffect(focusOccurrence, tasksState.rows) {
        val target = focusOccurrence ?: return@LaunchedEffect

        tab = Tab.Tasks
        leaf = Leaf.Root

        val row = tasksState.rows.firstOrNull { it.id == target } ?: return@LaunchedEffect

        viewModel.focus(row)
        onFocusHandled()
    }

    LaunchedEffect(state.writeTick) {
        ReminderSync.enqueue(context, force = state.writeTick > 0)

        if (state.writeTick > 0) {
            WidgetBridge.onWrote(context)
        }
    }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showBriefly(it)
            viewModel.dismissNotice()
        }
    }

    LaunchedEffect(preferences.expandRecurringInGrid) {
        viewModel.setExpandRecurringTasks(preferences.expandRecurringInGrid)
    }

    LaunchedEffect(preferences.digestEnabled, preferences.digestMinuteOfDay, preferences.notifyOverdue) {
        DigestScheduler(context).sync()
    }

    BackHandler(enabled = leaf != Leaf.Root || tab != Tab.Tasks) {
        when {
            leaf != Leaf.Root -> leaf = Leaf.Root
            else -> tab = Tab.Tasks
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.ground,
        snackbarHost = { ChronicleToastHost(snackbar) },
        bottomBar = {
            if (leaf == Leaf.Root) {
                TabBar(
                    items = Tab.entries.map { entry ->
                        entry.item(badge = if (entry == Tab.Tasks) tasksState.countOf(TaskFilter.Today) else null)
                    },
                    selected = tab.ordinal,
                    onSelect = { index -> tab = Tab.entries[index] },
                )
            }
        },
        floatingActionButton = {
            if (leaf == Leaf.Root && tab == Tab.Calendar) {
                ActionButton(
                    icon = Lucide.Plus,
                    label = "Novo evento",
                    onClick = viewModel::newEvent,
                )
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets)) {
            when {
                leaf == Leaf.Agenda -> AgendaHost(
                    viewModel = agendaViewModel,
                    categories = state.categories,
                    writeTick = state.writeTick,
                    snackbar = snackbar,
                    onOpen = viewModel::focus,
                    onToggle = { entry ->
                        agendaViewModel.markCompleted(entry.id, !entry.completed)
                        viewModel.toggleCompletion(entry)
                        tasksViewModel.markStale()
                    },
                    onLeave = { leaf = Leaf.Root },
                )

                leaf == Leaf.Categories -> CategoriesHost(
                    api = api,
                    snackbar = snackbar,
                    onLeave = { changed ->
                        leaf = Leaf.Root

                        if (changed) {
                            viewModel.refreshCategories()
                            viewModel.reload()
                            tasksViewModel.markStale()
                        }
                    },
                )

                tab == Tab.Tasks -> TasksHost(
                    viewModel = tasksViewModel,
                    categories = state.categories,
                    writeTick = state.writeTick,
                    snackbar = snackbar,
                    onOpen = viewModel::focus,
                    onDetailedAdd = viewModel::newTask,
                    onNeedsCode = viewModel::askForCode,
                    onWrote = { viewModel.reload() },
                )

                tab == Tab.Calendar -> MonthScreen(
                    state = state,
                    onSelect = viewModel::select,
                    onShowMonth = viewModel::showMonth,
                    onPickMonth = { pickerOpen = true },
                    onToday = viewModel::today,
                    onRetry = viewModel::reload,
                    onOpenEntry = viewModel::focus,
                    onToggleEntry = { entry ->
                        viewModel.toggleCompletion(entry)
                        WidgetBridge.onCompletionChanged(context, entry.id, !entry.completed)
                        tasksViewModel.markStale()
                    },
                    onAccount = { tab = Tab.Settings },
                    onOpenAgenda = { leaf = Leaf.Agenda },
                    accountInitial = accountInitial(profile.first, profile.second),
                    accountPhoto = photo,
                )

                else -> SettingsScreen(
                    name = profile.first,
                    email = profile.second,
                    initial = accountInitial(profile.first, profile.second),
                    photo = photo,
                    preferences = preferences,
                    unlocked = state.unlocked,
                    exactAlarmsAllowed = ExactAlarms.allowed(context),
                    canRequestExactAlarms = ExactAlarms.requestable(),
                    onPreferences = onPreferences,
                    onAskCode = viewModel::askForCode,
                    onLock = viewModel::lock,
                    onRequestExactAlarms = { ExactAlarms.request(context) },
                    onCategories = { leaf = Leaf.Categories },
                    onSignOut = onSignedOut,
                )
            }
        }
    }

    if (pickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { pickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            MonthPickerSheet(
                current = state.month,
                onPick = { month ->
                    pickerOpen = false
                    viewModel.jumpTo(month)
                },
            )
        }
    }

    if (state.askCode) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissCodePrompt,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            AccessCodeSheet(
                submitting = state.unlocking,
                error = state.unlockError,
                onSubmit = viewModel::submitCode,
                onDismiss = viewModel::dismissCodePrompt,
            )
        }
    }

    state.focused?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.focus(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            EntrySheet(
                entry = entry,
                detail = state.focusedDetail,
                onEdit = viewModel::editFocused,
                onToggleCompletion = {
                    viewModel.toggleCompletion(entry)
                    WidgetBridge.onCompletionChanged(context, entry.id, !entry.completed)
                    tasksViewModel.markStale()
                    viewModel.focus(null)
                },
                onCancelOccurrence = viewModel::cancelFocusedOccurrence,
                onDeleteSeries = viewModel::deleteFocusedSeries,
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
                onSave = {
                    agendaViewModel.markStale()
                    tasksViewModel.markStale()
                    viewModel.saveDraft()
                },
                onCancel = viewModel::closeDraft,
            )
        }
    }
}

private fun accountInitial(name: String?, email: String?): String {
    val source = name?.trim()?.takeIf { it.isNotEmpty() } ?: email?.trim()

    return source?.firstOrNull()?.uppercase() ?: "?"
}
