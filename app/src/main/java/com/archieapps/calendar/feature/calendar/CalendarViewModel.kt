package com.archieapps.calendar.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.net.CategoryDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val entries: Map<LocalDate, List<CalendarEntry>> = emptyMap(),
    val categories: List<CategoryDto> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val draft: EventDraft? = null,
    val focused: CalendarEntry? = null,
    val writeTick: Int = 0,
) {
    val gridStart: LocalDate
        get() = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    val gridDays: List<LocalDate>
        get() = (0 until 42).map { gridStart.plusDays(it.toLong()) }

    fun entriesOn(date: LocalDate): List<CalendarEntry> = entries[date].orEmpty()

    val selectedEntries: List<CalendarEntry> get() = entriesOn(selected)

    fun categoryName(id: Int?): String? = categories.firstOrNull { it.id == id }?.name
}

class CalendarViewModel(private val api: CalendarApi) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        load()
        loadCategories()
    }

    fun select(date: LocalDate) {
        val month = YearMonth.from(date)
        val changedMonth = month != _state.value.month

        _state.update { it.copy(selected = date, month = month) }

        if (changedMonth) load()
    }

    fun goToMonth(offset: Long) {
        val month = _state.value.month.plusMonths(offset)
        val selected = if (month == YearMonth.from(LocalDate.now())) LocalDate.now() else month.atDay(1)

        _state.update { it.copy(month = month, selected = selected) }
        load()
    }

    fun today() = select(LocalDate.now())

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun load() {
        val start = _state.value.gridStart
        val end = start.plusDays(41)

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = api.occurrences(start.toString(), end.toString())) {
                is ApiResult.Ok ->
                    _state.update {
                        it.copy(
                            entries = result.value.map { dto -> dto.toEntry() }.groupBy { entry -> entry.date },
                            loading = false,
                            error = null,
                        )
                    }

                is ApiResult.Failure ->
                    _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val result = api.categories()

            if (result is ApiResult.Ok) {
                _state.update { it.copy(categories = result.value) }
            }
        }
    }

    fun focus(entry: CalendarEntry?) = _state.update { it.copy(focused = entry) }

    fun newEvent() {
        val snapshot = _state.value
        val fallback = snapshot.categories.firstOrNull { it.isDefault }?.id

        _state.update { it.copy(draft = EventDraft.forDay(snapshot.selected, fallback), focused = null) }
    }

    fun editFocused() {
        val entry = _state.value.focused ?: return
        val eventId = entry.eventId ?: return

        _state.update { it.copy(focused = null, saving = true) }

        viewModelScope.launch {
            when (val result = api.event(eventId)) {
                is ApiResult.Ok ->
                    _state.update { it.copy(draft = EventDraft.from(result.value), saving = false) }

                is ApiResult.Failure ->
                    _state.update { it.copy(saving = false, error = result.message) }
            }
        }
    }

    fun updateDraft(transform: (EventDraft) -> EventDraft) {
        _state.update { current -> current.draft?.let { current.copy(draft = transform(it)) } ?: current }
    }

    fun closeDraft() = _state.update { it.copy(draft = null) }

    fun saveDraft() {
        val draft = _state.value.draft ?: return
        if (!draft.canSave) return

        _state.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            val payload = draft.toPayload()
            val result = draft.eventId?.let { api.updateEvent(it, payload) } ?: api.createEvent(payload)

            finish(result, if (draft.isEditing) "Evento atualizado." else "Evento criado.") {
                _state.update { it.copy(draft = null, selected = draft.date, month = YearMonth.from(draft.date)) }
            }
        }
    }

    fun toggleCompletion(entry: CalendarEntry) {
        if (!entry.isTask || entry.agency != Agency.Mine) return

        _state.update { it.copy(saving = true) }

        viewModelScope.launch {
            val result = api.setCompletion(entry.id, !entry.completed)

            finish(result, if (entry.completed) "Reaberto." else "Concluído.")
        }
    }

    fun cancelFocusedOccurrence() {
        val entry = _state.value.focused ?: return

        _state.update { it.copy(saving = true, focused = null) }

        viewModelScope.launch {
            finish(api.cancelOccurrence(entry.id), "Ocorrência removida.")
        }
    }

    fun deleteFocusedSeries() {
        val entry = _state.value.focused ?: return
        val eventId = entry.eventId ?: return

        _state.update { it.copy(saving = true, focused = null) }

        viewModelScope.launch {
            finish(api.deleteEvent(eventId), "Evento removido.")
        }
    }

    private fun finish(result: ApiResult<Unit>, success: String, onOk: () -> Unit = {}) {
        when (result) {
            is ApiResult.Ok -> {
                onOk()
                _state.update { it.copy(saving = false, notice = success, writeTick = it.writeTick + 1) }
                load()
            }

            is ApiResult.Failure ->
                _state.update { it.copy(saving = false, error = result.message) }
        }
    }
}
