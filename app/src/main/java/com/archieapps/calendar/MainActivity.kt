package com.archieapps.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.alarm.ExactAlarms
import com.archieapps.calendar.core.media.AvatarLoader
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.sync.ReminderSync
import com.archieapps.calendar.design.CalendarTheme
import java.time.LocalDate
import java.time.YearMonth
import com.archieapps.calendar.design.Eyebrow
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.feature.agenda.AgendaFilterSheet
import com.archieapps.calendar.feature.agenda.AgendaScreen
import com.archieapps.calendar.feature.agenda.AgendaViewModel
import com.archieapps.calendar.feature.auth.AccessCodeSheet
import com.archieapps.calendar.feature.auth.AccountSheet
import com.archieapps.calendar.feature.auth.LoginScreen
import com.archieapps.calendar.feature.auth.LoginViewModel
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.CalendarViewModel
import com.archieapps.calendar.feature.calendar.EntrySheet
import com.archieapps.calendar.feature.calendar.EventEditor
import com.archieapps.calendar.feature.calendar.MonthPickerSheet
import com.archieapps.calendar.feature.calendar.MonthScreen
import com.archieapps.calendar.feature.categories.CategoriesScreen
import com.archieapps.calendar.feature.categories.CategoriesViewModel
import com.archieapps.calendar.feature.categories.CategoryDeletePrompt
import com.archieapps.calendar.feature.categories.CategoryEditor

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

    Box(Modifier.fillMaxSize().background(colors.ground)) {
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
    val api = remember(settings) { CalendarApi(settings) }
    val viewModel: CalendarViewModel = viewModel(
        key = "calendar",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarViewModel(api) as T
        }
    )

    val agendaViewModel: AgendaViewModel = viewModel(
        key = "agenda",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AgendaViewModel(api) as T
        }
    )

    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var route by remember { mutableStateOf(Route.Month) }
    var accountOpen by remember { mutableStateOf(false) }
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

    LaunchedEffect(state.unauthorized) {
        if (state.unauthorized) onSignedOut()
    }

    LaunchedEffect(state.writeTick) {
        ReminderSync.enqueue(context, force = state.writeTick > 0)
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
            if (route == Route.Month) {
                FloatingActionButton(
                    onClick = viewModel::newEvent,
                    containerColor = colors.brand,
                    contentColor = if (colors.isDark) colors.ground else Color.White,
                ) {
                    Text("+", style = Eyebrow)
                }
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets)) {
            when (route) {
                Route.Month -> MonthScreen(
                    state = state,
                    onSelect = viewModel::select,
                    onShowMonth = viewModel::showMonth,
                    onPickMonth = { pickerOpen = true },
                    onToday = viewModel::today,
                    onRetry = viewModel::reload,
                    onOpenEntry = viewModel::focus,
                    onToggleEntry = viewModel::toggleCompletion,
                    onAccount = { accountOpen = true },
                    onOpenAgenda = { route = Route.Agenda },
                    accountInitial = accountInitial(profile.first, profile.second),
                    accountPhoto = photo,
                )

                Route.Agenda -> Agenda(
                    viewModel = agendaViewModel,
                    categories = state.categories,
                    writeTick = state.writeTick,
                    snackbar = snackbar,
                    onOpen = viewModel::focus,
                    onToggle = { entry ->
                        agendaViewModel.markCompleted(entry.id, !entry.completed)
                        viewModel.toggleCompletion(entry)
                    },
                    onLeave = { route = Route.Month },
                )

                Route.Categories -> Categories(
                    api = api,
                    snackbar = snackbar,
                    onLeave = { changed ->
                        route = Route.Month

                        if (changed) {
                            viewModel.refreshCategories()
                            viewModel.reload()
                        }
                    },
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

    if (accountOpen) {
        ModalBottomSheet(
            onDismissRequest = { accountOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            AccountSheet(
                name = profile.first,
                email = profile.second,
                photo = photo,
                initial = accountInitial(profile.first, profile.second),
                unlocked = state.unlocked,
                onAskCode = {
                    accountOpen = false
                    viewModel.askForCode()
                },
                onLock = viewModel::lock,
                exactAlarmsAllowed = ExactAlarms.allowed(context),
                canRequestExactAlarms = ExactAlarms.requestable(),
                onRequestExactAlarms = { ExactAlarms.request(context) },
                onCategories = {
                    accountOpen = false
                    route = Route.Categories
                },
                onSignOut = {
                    accountOpen = false
                    onSignedOut()
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
                    viewModel.saveDraft()
                },
                onCancel = viewModel::closeDraft,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Categories(
    api: CalendarApi,
    snackbar: SnackbarHostState,
    onLeave: (Boolean) -> Unit,
) {
    val colors = LocalChronicle.current
    val viewModel: CategoriesViewModel = viewModel(
        key = "categories",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CategoriesViewModel(api) as T
        }
    )

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.enter()
    }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissNotice()
        }
    }

    LaunchedEffect(state.unauthorized) {
        if (state.unauthorized) onLeave(true)
    }

    BackHandler { onLeave(state.dirty) }

    CategoriesScreen(
        state = state,
        onBack = { onLeave(state.dirty) },
        onNew = viewModel::newCategory,
        onEdit = viewModel::edit,
        onToggleArchive = viewModel::toggleArchive,
        onDelete = viewModel::askDelete,
        onMove = viewModel::move,
        onRetry = viewModel::load,
    )

    state.draft?.let { draft ->
        ModalBottomSheet(
            onDismissRequest = viewModel::closeDraft,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            CategoryEditor(
                draft = draft,
                category = state.items.firstOrNull { it.id == draft.id },
                saving = state.saving,
                onChange = viewModel::updateDraft,
                onSave = viewModel::save,
                onCancel = viewModel::closeDraft,
            )
        }
    }

    state.pendingDelete?.let { category ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissDelete,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            CategoryDeletePrompt(
                category = category,
                targets = state.reassignTargets,
                onReassign = viewModel::confirmDelete,
                onForce = { viewModel.confirmDelete(null) },
                onDismiss = viewModel::dismissDelete,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Agenda(
    viewModel: AgendaViewModel,
    categories: List<CategoryDto>,
    writeTick: Int,
    snackbar: SnackbarHostState,
    onOpen: (CalendarEntry) -> Unit,
    onToggle: (CalendarEntry) -> Unit,
    onLeave: () -> Unit,
) {
    val colors = LocalChronicle.current
    val state by viewModel.state.collectAsState()
    var seenTick by remember { mutableStateOf(writeTick) }

    LaunchedEffect(Unit) {
        viewModel.enter()
    }

    LaunchedEffect(writeTick) {
        if (writeTick != seenTick) {
            seenTick = writeTick
            viewModel.reloadIfStale()
        }
    }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissNotice()
        }
    }

    BackHandler { onLeave() }

    AgendaScreen(
        state = state,
        onBack = onLeave,
        onQuery = viewModel::onQuery,
        onSubmitQuery = viewModel::submitQuery,
        onOpenFilters = viewModel::openFilters,
        onClearFilters = viewModel::clearFilters,
        onOpen = onOpen,
        onToggle = onToggle,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        onRetryTail = viewModel::retryTail,
    )

    if (state.filtersOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeFilters,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            AgendaFilterSheet(
                filters = state.filters,
                categories = categories,
                onFrom = viewModel::setFrom,
                onToday = viewModel::anchorToday,
                onPickMonth = viewModel::openMonthPicker,
                onToggleCategory = viewModel::toggleCategory,
                onToggleKind = viewModel::toggleKind,
                onToggleNature = viewModel::toggleNature,
                onClear = viewModel::clearFilters,
            )
        }
    }

    if (state.monthPickerOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeMonthPicker,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            MonthPickerSheet(
                current = YearMonth.from(state.filters.from),
                onPick = { month ->
                    viewModel.closeMonthPicker()
                    viewModel.setFrom(
                        if (month == YearMonth.now()) LocalDate.now() else month.atDay(1),
                    )
                },
            )
        }
    }
}

private enum class Route { Month, Categories, Agenda }

private fun accountInitial(name: String?, email: String?): String {
    val source = name?.trim()?.takeIf { it.isNotEmpty() } ?: email?.trim()

    return source?.firstOrNull()?.uppercase() ?: "?"
}
