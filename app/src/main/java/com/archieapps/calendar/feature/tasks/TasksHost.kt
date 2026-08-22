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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.components.showBriefly
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.TaskAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksHost(
    viewModel: TasksViewModel,
    categories: List<CategoryDto>,
    writeTick: Int,
    snackbar: SnackbarHostState,
    onOpen: (CalendarEntry) -> Unit,
    onDetailedAdd: () -> Unit,
    onNeedsCode: () -> Unit,
    onWrote: () -> Unit,
) {
    val colors = LocalChronicle.current
    val scope = rememberCoroutineScope()
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

    LaunchedEffect(state.needsCode) {
        if (state.needsCode) {
            onNeedsCode()
            viewModel.codePrompted()
        }
    }

    LaunchedEffect(state.noticeTick) {
        if (state.noticeTick == 0) return@LaunchedEffect

        val message = state.notice ?: return@LaunchedEffect
        val undoable = state.undo != null

        val result = snackbar.showBriefly(
            message = message,
            actionLabel = if (undoable) "desfazer" else null,
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
        onDetailedAdd = onDetailedAdd,
        onOpen = onOpen,
        onToggle = { entry ->
            viewModel.toggleCompletion(entry)
            onWrote()
        },
        onActionFailed = { action ->
            scope.launch { snackbar.showBriefly("não foi possível abrir ${action.caption()}") }
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
