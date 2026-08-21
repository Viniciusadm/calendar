package com.archieapps.calendar.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.AccessCode
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.net.CategoryDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesState(
    val items: List<CategoryDto> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val draft: CategoryDraft? = null,
    val pendingDelete: CategoryDto? = null,
    val dirty: Boolean = false,
    val unauthorized: Boolean = false,
    val askCode: Boolean = false,
    val unlocking: Boolean = false,
    val unlockError: String? = null,
) {
    val reassignTargets: List<CategoryDto>
        get() = items.filter { it.active && it.id != pendingDelete?.id }
}

class CategoriesViewModel(private val api: CalendarApi) : ViewModel() {
    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    private var reorderJob: Job? = null

    fun enter() {
        _state.update { it.copy(dirty = false) }
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = api.categories(includeArchived = true, withCounts = true)) {
                is ApiResult.Ok ->
                    _state.update { it.copy(items = result.value, loading = false) }

                is ApiResult.Failure ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = result.message,
                            unauthorized = it.unauthorized || result.unauthorized,
                        )
                    }
            }
        }
    }

    fun newCategory() = _state.update { it.copy(draft = CategoryDraft(), error = null) }

    fun edit(category: CategoryDto) =
        _state.update { it.copy(draft = CategoryDraft.from(category), error = null) }

    fun updateDraft(transform: (CategoryDraft) -> CategoryDraft) {
        _state.update { current -> current.draft?.let { current.copy(draft = transform(it)) } ?: current }
    }

    fun closeDraft() = _state.update { it.copy(draft = null) }

    fun save() {
        val draft = _state.value.draft ?: return
        if (!draft.canSave) return

        _state.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            val payload = draft.toPayload()
            val result = draft.id?.let { api.updateCategory(it, payload) } ?: api.createCategory(payload)

            finish(result, if (draft.isEditing) "Categoria atualizada." else "Categoria criada.") {
                _state.update { it.copy(draft = null) }
            }
        }
    }

    fun toggleArchive(category: CategoryDto) {
        _state.update { it.copy(saving = true, draft = null, error = null) }

        viewModelScope.launch {
            val active = !category.active

            finish(
                api.archiveCategory(category.id, active),
                if (active) "Categoria reativada." else "Categoria arquivada.",
            )
        }
    }

    fun askDelete(category: CategoryDto) =
        _state.update { it.copy(pendingDelete = category, draft = null, error = null) }

    fun dismissDelete() = _state.update { it.copy(pendingDelete = null) }

    fun confirmDelete(reassignTo: Int?) {
        val category = _state.value.pendingDelete ?: return

        _state.update { it.copy(saving = true, pendingDelete = null, error = null) }

        viewModelScope.launch {
            finish(api.deleteCategory(category.id, reassignTo), "Categoria removida.")
        }
    }

    fun move(index: Int, delta: Int) {
        val items = _state.value.items
        val target = index + delta

        if (index !in items.indices || target !in items.indices) return

        val reordered = items.toMutableList().apply { add(target, removeAt(index)) }

        _state.update { it.copy(items = reordered, error = null) }

        val previous = reorderJob

        reorderJob = viewModelScope.launch {
            previous?.join()

            when (val result = api.reorderCategories(reordered.map { it.id })) {
                is ApiResult.Ok -> _state.update { it.copy(dirty = true) }

                is ApiResult.Failure -> {
                    report(result)
                    load()
                }
            }
        }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun dismissCodePrompt() = _state.update { it.copy(askCode = false, unlockError = null) }

    fun submitCode(code: String) {
        _state.update { it.copy(unlocking = true, unlockError = null) }

        viewModelScope.launch {
            when (val result = api.validateCode(code)) {
                is ApiResult.Ok -> {
                    AccessCode.remember(code)
                    _state.update { it.copy(unlocking = false, askCode = false) }
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(unlocking = false, unlockError = result.message) }
            }
        }
    }

    private fun finish(result: ApiResult<Unit>, success: String, onOk: () -> Unit = {}) {
        when (result) {
            is ApiResult.Ok -> {
                onOk()
                _state.update { it.copy(saving = false, notice = success, dirty = true) }
                load()
            }

            is ApiResult.Failure -> {
                _state.update { it.copy(saving = false) }
                report(result)
            }
        }
    }

    private fun report(failure: ApiResult.Failure) {
        _state.update {
            it.copy(
                error = if (failure.gated) null else failure.message,
                askCode = it.askCode || failure.gated,
                unauthorized = it.unauthorized || failure.unauthorized,
            )
        }
    }
}
