package com.archieapps.calendar.feature.tasks

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksHost(
    viewModel: TasksViewModel,
    categories: List<CategoryDto>,
    writeTick: Int,
    snackbar: SnackbarHostState,
    onOpen: (CalendarEntry) -> Unit,
    onWrote: () -> Unit,
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

    LaunchedEffect(state.noticeTick) {
        if (state.noticeTick == 0) return@LaunchedEffect

        val message = state.notice ?: return@LaunchedEffect
        val undoable = state.undo != null

        val result = snackbar.showSnackbar(
            message = message,
            actionLabel = if (undoable) "desfazer" else null,
            withDismissAction = false,
        )

        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undo()
        } else {
            viewModel.dismissNotice()
        }
    }

    TasksScreen(
        state = state,
        onFilter = viewModel::setFilter,
        onQuery = viewModel::onQuery,
        onSubmitQuery = viewModel::submitQuery,
        onClearFilters = viewModel::clearFilters,
        onOpenFilters = viewModel::openFilters,
        onQuickAdd = { title ->
            viewModel.quickAdd(title)
            onWrote()
        },
        onOpen = onOpen,
        onToggle = { entry ->
            viewModel.toggleCompletion(entry)
            onWrote()
        },
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
            TaskFilterSheet(
                state = state,
                categories = categories,
                onToggleCategory = viewModel::toggleCategory,
                onTogglePriority = viewModel::togglePriority,
                onClear = viewModel::clearFilters,
            )
        }
    }
}
