package com.archieapps.calendar.feature.categories

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.feature.auth.AccessCodeSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesHost(
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
