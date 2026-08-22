package com.archieapps.calendar.feature.agenda

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.MonthPickerSheet
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaHost(
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
