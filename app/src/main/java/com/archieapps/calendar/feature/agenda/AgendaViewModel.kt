package com.archieapps.calendar.feature.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.toEntry
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgendaViewModel(private val api: CalendarApi) : ViewModel() {
    private val _state = MutableStateFlow(AgendaState())
    val state: StateFlow<AgendaState> = _state.asStateFlow()

    private var pageJob: Job? = null
    private var typeJob: Job? = null
    private var generation = 0

    fun enter() {
        val snapshot = _state.value

        if (snapshot.days.isEmpty() || snapshot.stale || snapshot.loadedOn != LocalDate.now()) {
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

    fun onQuery(text: String) {
        _state.update { it.copy(draftQuery = text) }

        typeJob?.cancel()
        typeJob = viewModelScope.launch {
            delay(searchSettleMs)
            change(settle = false) { it.copy(query = text.trim()) }
        }
    }

    fun submitQuery() {
        typeJob?.cancel()
        change(settle = false) { it.copy(query = _state.value.draftQuery.trim()) }
    }

    fun setFrom(date: LocalDate) {
        change(settle = true) { it.copy(from = date) }
    }

    fun anchorToday() {
        setFrom(LocalDate.now())
    }

    fun toggleCategory(id: Int) {
        change(settle = true) { filters ->
            filters.copy(
                categoryIds = if (filters.categoryIds.contains(id)) {
                    filters.categoryIds - id
                } else {
                    filters.categoryIds + id
                },
            )
        }
    }

    fun toggleKind(value: String) {
        change(settle = true) { filters ->
            val next = toggled(filters.kinds, value) ?: return@change filters

            filters.copy(kinds = next)
        }
    }

    fun toggleNature(value: String) {
        change(settle = true) { filters ->
            val next = toggled(filters.natures, value) ?: return@change filters

            filters.copy(natures = next)
        }
    }

    fun clearFilters() {
        typeJob?.cancel()
        _state.update { it.copy(draftQuery = "") }
        change(settle = false) { AgendaFilters() }
    }

    fun openFilters() {
        _state.update { it.copy(filtersOpen = true) }
    }

    fun closeFilters() {
        _state.update { it.copy(filtersOpen = false) }
    }

    fun openMonthPicker() {
        _state.update { it.copy(filtersOpen = false, monthPickerOpen = true) }
    }

    fun closeMonthPicker() {
        _state.update { it.copy(monthPickerOpen = false) }
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

    fun markCompleted(occurrenceId: String, completed: Boolean) {
        _state.update { it.copy(days = it.days.patchCompletion(occurrenceId, completed)) }
    }

    fun dropOccurrence(occurrenceId: String) {
        _state.update { it.copy(days = it.days.withoutOccurrence(occurrenceId)) }
    }

    fun dropEvent(eventId: Int?) {
        if (eventId == null) {
            return
        }

        _state.update { it.copy(days = it.days.withoutEvent(eventId)) }
    }

    fun dismissNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun change(settle: Boolean, transform: (AgendaFilters) -> AgendaFilters) {
        val current = _state.value.filters
        val next = transform(current)

        if (next == current) {
            return
        }

        _state.update { it.copy(filters = next, error = null, tailError = null) }
        reload(settle = settle)
    }

    private fun toggled(current: Set<String>, value: String): Set<String>? {
        if (!current.contains(value)) {
            return current + value
        }

        if (current.size == 1) {
            return null
        }

        return current - value
    }

    private fun fetch(fresh: Boolean, settle: Boolean) {
        val token = generation
        val snapshot = _state.value
        val filters = snapshot.filters

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

            val result = api.agenda(
                from = filters.from.toString(),
                cursor = if (fresh) null else snapshot.cursor,
                query = filters.queryParam,
                categories = filters.categoriesParam,
                kinds = filters.kindsParam,
                natures = filters.naturesParam,
            )

            if (token != generation) {
                return@launch
            }

            when (result) {
                is ApiResult.Ok -> {
                    val entries = result.value.map { it.toEntry() }

                    _state.update {
                        applyPage(
                            state = it,
                            entries = entries,
                            hasMore = result.misc?.hasMore ?: false,
                            nextCursor = result.misc?.nextCursor,
                            fresh = fresh,
                            today = LocalDate.now(),
                        )
                    }
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
}

private const val searchSettleMs = 320L

private const val filterSettleMs = 250L
