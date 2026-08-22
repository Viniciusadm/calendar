package com.archieapps.calendar.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.EventDraft
import com.archieapps.calendar.feature.calendar.togglable
import com.archieapps.calendar.feature.calendar.toEntry
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TasksViewModel(
    private val api: CalendarApi,
    initialFilter: TaskFilter = TaskFilter.Today,
) : ViewModel() {
    private val _state = MutableStateFlow(TaskListState(filter = initialFilter))
    val state: StateFlow<TaskListState> = _state.asStateFlow()

    private var pageJob: Job? = null
    private var typeJob: Job? = null
    private var generation = 0

    fun enter() {
        val snapshot = _state.value

        if (snapshot.rows.isEmpty() || snapshot.stale || snapshot.loadedOn != LocalDate.now()) {
            reload(settle = false)
        }
    }

    fun markStale() {
        _state.update { it.copy(stale = true) }
    }

    fun reloadIfStale() {
        if (_state.value.stale) {
            reload(settle = false)
        }
    }

    fun setFilter(filter: TaskFilter) {
        if (_state.value.filter == filter) {
            return
        }

        _state.update { it.copy(filter = filter, error = null, tailError = null) }
        reload(settle = false)
    }

    fun onQuery(text: String) {
        _state.update { it.copy(draftQuery = text) }

        typeJob?.cancel()
        typeJob = viewModelScope.launch {
            delay(searchSettleMs)
            commitQuery(text)
        }
    }

    fun submitQuery() {
        typeJob?.cancel()
        commitQuery(_state.value.draftQuery)
    }

    fun toggleCategory(id: Int) {
        _state.update { current ->
            val next = if (current.categoryIds.contains(id)) {
                current.categoryIds - id
            } else {
                current.categoryIds + id
            }

            current.copy(categoryIds = next)
        }

        reload(settle = true)
    }

    fun togglePriority(slug: String) {
        _state.update { current ->
            val next = if (current.priorities.contains(slug)) {
                current.priorities - slug
            } else {
                current.priorities + slug
            }

            current.copy(priorities = next)
        }

        reload(settle = true)
    }

    fun clearFilters() {
        typeJob?.cancel()
        _state.update {
            it.copy(draftQuery = "", query = "", categoryIds = emptySet(), priorities = emptySet())
        }
        reload(settle = false)
    }

    fun openFilters() {
        _state.update { it.copy(filtersOpen = true) }
    }

    fun closeFilters() {
        _state.update { it.copy(filtersOpen = false) }
    }

    fun loadMore() {
        if (!_state.value.canLoadMore || pageJob?.isActive == true) {
            return
        }

        fetch(fresh = false, settle = false)
    }

    fun retry() {
        reload(settle = false)
    }

    fun retryTail() {
        _state.update { it.copy(tailError = null) }
        fetch(fresh = false, settle = false)
    }

    fun reload(settle: Boolean) {
        generation++
        fetch(fresh = true, settle = settle)
    }

    fun toggleCompletion(entry: CalendarEntry) {
        if (!entry.togglable() || _state.value.saving) {
            return
        }

        val target = !entry.completed

        _state.update {
            it.copy(
                saving = true,
                rows = it.rows.patchRowCompletion(entry.id, target),
            )
        }

        viewModelScope.launch {
            when (val result = api.setCompletion(entry.id, target)) {
                is ApiResult.Ok -> settle(
                    notice = if (target) "Concluída." else "Reaberta.",
                    undo = TaskUndo(entry.id, UndoKind.Completion, !target, entry.title),
                )

                is ApiResult.Failure -> fail(result) {
                    it.copy(rows = it.rows.patchRowCompletion(entry.id, !target))
                }
            }
        }
    }

    fun cancelOccurrence(entry: CalendarEntry) {
        if (!entry.recurring || _state.value.saving) {
            return
        }

        _state.update { it.copy(saving = true, rows = it.rows.withoutRow(entry.id)) }

        viewModelScope.launch {
            when (val result = api.cancelOccurrence(entry.id)) {
                is ApiResult.Ok -> settle(
                    notice = "Ocorrência cancelada.",
                    undo = TaskUndo(entry.id, UndoKind.Cancellation, false, entry.title),
                )

                is ApiResult.Failure -> fail(result) { it }
            }
        }
    }

    fun deleteSeries(entry: CalendarEntry) {
        val eventId = entry.eventId

        if (eventId == null || _state.value.saving) {
            return
        }

        _state.update { it.copy(saving = true, rows = it.rows.withoutSeries(eventId)) }

        viewModelScope.launch {
            when (val result = api.deleteEvent(eventId)) {
                is ApiResult.Ok -> settle(notice = "Tarefa removida.", undo = null)
                is ApiResult.Failure -> fail(result) { it }
            }
        }
    }

    fun quickAdd(title: String) {
        val trimmed = title.trim()

        if (trimmed.isEmpty() || _state.value.saving) {
            return
        }

        _state.update { it.copy(saving = true) }

        val payload = EventDraft(
            title = trimmed,
            date = LocalDate.now(),
            allDay = true,
            isTask = true,
        ).toPayload()

        viewModelScope.launch {
            when (val result = api.createEvent(payload)) {
                is ApiResult.Ok -> settle(notice = "Tarefa criada.", undo = null)
                is ApiResult.Failure -> fail(result) { it }
            }
        }
    }

    fun undo() {
        val pending = _state.value.undo ?: return

        _state.update { it.copy(saving = true, undo = null) }

        viewModelScope.launch {
            val result = when (pending.kind) {
                UndoKind.Completion -> api.setCompletion(pending.occurrenceId, pending.completed)
                UndoKind.Cancellation -> api.resetOccurrence(pending.occurrenceId)
            }

            when (result) {
                is ApiResult.Ok -> settle(notice = "Desfeito.", undo = null)
                is ApiResult.Failure -> fail(result) { it }
            }
        }
    }

    fun dismissNotice() {
        _state.update { it.copy(notice = null, undo = null) }
    }

    private fun commitQuery(text: String) {
        val trimmed = text.trim()

        if (_state.value.query == trimmed) {
            return
        }

        _state.update { it.copy(query = trimmed, error = null, tailError = null) }
        reload(settle = false)
    }

    private fun settle(notice: String, undo: TaskUndo?) {
        _state.update {
            it.copy(saving = false, notice = notice, noticeTick = it.noticeTick + 1, undo = undo)
        }
        reload(settle = false)
    }

    private fun fail(
        failure: ApiResult.Failure,
        rollback: (TaskListState) -> TaskListState,
    ) {
        _state.update {
            rollback(it).copy(
                saving = false,
                notice = if (failure.gated) null else failure.message,
                noticeTick = if (failure.gated) it.noticeTick else it.noticeTick + 1,
                needsCode = it.needsCode || failure.gated,
                undo = null,
                unauthorized = it.unauthorized || failure.unauthorized,
            )
        }

        if (failure.gated) {
            reload(settle = false)
        }
    }

    fun codePrompted() {
        _state.update { it.copy(needsCode = false) }
    }

    private fun fetch(fresh: Boolean, settle: Boolean) {
        val token = generation
        val snapshot = _state.value
        val page = if (fresh) 1 else snapshot.page + 1

        _state.update {
            if (fresh) {
                it.copy(loading = true, appending = false, error = null, tailError = null)
            } else {
                it.copy(appending = true, tailError = null)
            }
        }

        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            if (settle) {
                delay(filterSettleMs)
            }

            val result = api.tasks(
                filter = snapshot.filter.value,
                page = page,
                query = snapshot.query.ifBlank { null },
                categories = snapshot.categoriesParam,
                priorities = snapshot.prioritiesParam,
            )

            if (token != generation) {
                return@launch
            }

            when (result) {
                is ApiResult.Ok -> {
                    val rows = result.value.map { it.toEntry() }
                    val info = result.misc?.page

                    _state.update {
                        it.copy(
                            rows = mergeRows(it.rows, rows, fresh),
                            page = info?.current ?: page,
                            lastPage = info?.last ?: page,
                            total = info?.total ?: rows.size,
                            truncated = result.misc?.truncated ?: false,
                            loading = false,
                            appending = false,
                            error = null,
                            tailError = null,
                            loadedOn = LocalDate.now(),
                            stale = false,
                            resetTick = if (fresh) it.resetTick + 1 else it.resetTick,
                        )
                    }

                    refreshSummary(token)
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(
                        loading = false,
                        appending = false,
                        error = if (fresh) result.message else it.error,
                        tailError = if (fresh) null else result.message,
                        unauthorized = it.unauthorized || result.unauthorized,
                    )
                }
            }
        }
    }

    private suspend fun refreshSummary(token: Int) {
        val snapshot = _state.value

        val result = api.taskSummary(
            query = snapshot.query.ifBlank { null },
            categories = snapshot.categoriesParam,
            priorities = snapshot.prioritiesParam,
        )

        if (token != generation) {
            return
        }

        if (result is ApiResult.Ok) {
            _state.update { it.copy(summary = result.value) }
        }
    }
}

private const val searchSettleMs = 320L

private const val filterSettleMs = 250L
