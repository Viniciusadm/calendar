package com.archieapps.calendar.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.core.net.AccessCode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val months: Map<YearMonth, Map<LocalDate, List<CalendarEntry>>> = emptyMap(),
    val categories: List<CategoryDto> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val draft: EventDraft? = null,
    val focused: CalendarEntry? = null,
    val focusedDetail: DetailState = DetailState.Absent,
    val writeTick: Int = 0,
    val unauthorized: Boolean = false,
    val unlocked: Boolean = false,
    val unlocking: Boolean = false,
    val unlockError: String? = null,
    val askCode: Boolean = false,
) {
    fun entriesFor(month: YearMonth): Map<LocalDate, List<CalendarEntry>> = months[month].orEmpty()

    val selectedEntries: List<CalendarEntry> get() = entriesFor(month)[selected].orEmpty()
}

fun LocalDate.carriedInto(month: YearMonth): LocalDate {
    val today = LocalDate.now()

    if (month == YearMonth.from(today)) return today

    return month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
}

fun YearMonth.gridStart(): LocalDate =
    atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

fun YearMonth.gridDays(): List<LocalDate> {
    val start = gridStart()

    return (0 until 42).map { start.plusDays(it.toLong()) }
}

class CalendarViewModel(private val api: CalendarApi) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var loadJob: Job? = null

    private var detailJob: Job? = null

    init {
        _state.update { it.copy(unlocked = AccessCode.present) }
        ensure(_state.value.month, settle = false)
        refreshCategories()
    }

    fun askForCode() = _state.update { it.copy(askCode = true, unlockError = null) }

    fun dismissCodePrompt() = _state.update { it.copy(askCode = false, unlockError = null) }

    fun submitCode(code: String) {
        _state.update { it.copy(unlocking = true, unlockError = null) }

        viewModelScope.launch {
            when (val result = api.validateCode(code)) {
                is ApiResult.Ok -> {
                    AccessCode.remember(code)
                    _state.update { it.copy(unlocking = false, unlocked = true, askCode = false) }
                    ensure(_state.value.month, force = true, settle = false)
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(unlocking = false, unlockError = result.message) }
            }
        }
    }

    fun lock() {
        AccessCode.forget()
        _state.update { it.copy(unlocked = false, unlockError = null) }
        ensure(_state.value.month, force = true, settle = false)
    }

    fun select(date: LocalDate) = _state.update { it.copy(selected = date) }

    fun showMonth(month: YearMonth) = goTo(month, settle = true)

    fun jumpTo(month: YearMonth) = goTo(month, settle = false)

    private fun goTo(month: YearMonth, settle: Boolean) {
        val target = CalendarBounds.clamp(month)

        if (_state.value.month == target) return

        _state.update { it.copy(month = target, selected = it.selected.carriedInto(target)) }
        ensure(target, settle = settle)
    }

    fun today() {
        val now = LocalDate.now()

        _state.update { it.copy(selected = now, month = YearMonth.from(now)) }
        ensure(YearMonth.from(now), settle = false)
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun reload() = ensure(_state.value.month, force = true, settle = false)

    private fun ensure(month: YearMonth, force: Boolean = false, settle: Boolean = true) {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            if (settle) delay(SETTLE_MS)

            val range = (-PREFETCH_RADIUS..PREFETCH_RADIUS).map { month.plusMonths(it.toLong()) }

            _state.update { current -> current.copy(months = current.months.filterKeys { it in range }) }

            if (force || !_state.value.months.containsKey(month)) {
                if (!fetch(listOf(month), visible = true)) return@launch
            }

            fetch(pending(range.filter { it > month }, force), visible = false)
            fetch(pending(range.filter { it < month }, force), visible = false)
        }
    }

    private fun pending(months: List<YearMonth>, force: Boolean): List<YearMonth> =
        if (force) months else months.filterNot { _state.value.months.containsKey(it) }

    private suspend fun fetch(months: List<YearMonth>, visible: Boolean): Boolean {
        if (months.isEmpty()) return true

        val first = months.min()
        val last = months.max()
        val span = generateSequence(first) { previous -> previous.plusMonths(1).takeIf { it <= last } }.toList()
        val start = first.gridStart()
        val end = last.gridStart().plusDays(41)

        if (visible) _state.update { it.copy(loading = true, error = null) }

        return when (val result = api.occurrences(start.toString(), end.toString())) {
            is ApiResult.Ok -> {
                val byDate = result.value.map { dto -> dto.toEntry() }.groupBy { entry -> entry.date }

                _state.update { current ->
                    val cache = current.months.toMutableMap()

                    span.forEach { month ->
                        cache[month] = month.gridDays()
                            .mapNotNull { day -> byDate[day]?.let { day to it } }
                            .toMap()
                    }

                    current.copy(
                        months = cache,
                        loading = if (visible) false else current.loading,
                        error = if (visible) null else current.error,
                    )
                }

                true
            }

            is ApiResult.Failure -> {
                _state.update {
                    it.copy(
                        loading = if (visible) false else it.loading,
                        error = if (visible) result.message else it.error,
                        unauthorized = it.unauthorized || result.unauthorized,
                    )
                }

                false
            }
        }
    }

    fun refreshCategories() {
        viewModelScope.launch {
            val result = api.categories()

            if (result is ApiResult.Ok) {
                _state.update { it.copy(categories = result.value) }
            }
        }
    }

    fun focus(entry: CalendarEntry?) {
        detailJob?.cancel()

        val eventId = entry?.eventId

        if (eventId == null) {
            _state.update { it.copy(focused = entry, focusedDetail = DetailState.Absent) }

            return
        }

        _state.update { it.copy(focused = entry, focusedDetail = DetailState.Loading) }

        detailJob = viewModelScope.launch {
            val result = api.event(eventId)

            if (_state.value.focused?.eventId != eventId) return@launch

            val detail = when (result) {
                is ApiResult.Ok -> DetailState.Ready(result.value.toDetail())
                is ApiResult.Failure -> DetailState.Absent
            }

            _state.update { it.copy(focusedDetail = detail) }
        }
    }

    fun newEvent() {
        val snapshot = _state.value
        val fallback = snapshot.categories.firstOrNull { it.isDefault }?.id

        _state.update {
            it.copy(
                draft = EventDraft.forDay(snapshot.selected, fallback),
                focused = null,
                focusedDetail = DetailState.Absent,
            )
        }
    }

    fun editFocused() {
        val entry = _state.value.focused ?: return
        val eventId = entry.eventId ?: return

        _state.update { it.copy(focused = null, focusedDetail = DetailState.Absent, saving = true) }

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
                _state.update {
                    it.copy(draft = null, selected = draft.date, month = YearMonth.from(draft.date))
                }
            }
        }
    }

    fun toggleCompletion(entry: CalendarEntry) {
        if (!entry.isTask || entry.agency != Agency.Mine) return

        _state.update { it.copy(saving = true) }

        viewModelScope.launch {
            finish(api.setCompletion(entry.id, !entry.completed), if (entry.completed) "Reaberto." else "Concluído.")
        }
    }

    fun cancelFocusedOccurrence() {
        val entry = _state.value.focused ?: return

        _state.update { it.copy(saving = true, focused = null, focusedDetail = DetailState.Absent) }

        viewModelScope.launch {
            finish(api.cancelOccurrence(entry.id), "Ocorrência removida.")
        }
    }

    fun deleteFocusedSeries() {
        val entry = _state.value.focused ?: return
        val eventId = entry.eventId ?: return

        _state.update { it.copy(saving = true, focused = null, focusedDetail = DetailState.Absent) }

        viewModelScope.launch {
            finish(api.deleteEvent(eventId), "Evento removido.")
        }
    }

    private fun finish(result: ApiResult<Unit>, success: String, onOk: () -> Unit = {}) {
        when (result) {
            is ApiResult.Ok -> {
                onOk()
                _state.update { it.copy(saving = false, notice = success, writeTick = it.writeTick + 1) }
                ensure(_state.value.month, force = true, settle = false)
            }

            is ApiResult.Failure ->
                _state.update {
                    it.copy(
                        saving = false,
                        error = if (result.gated) null else result.message,
                        askCode = result.gated,
                        unauthorized = result.unauthorized,
                    )
                }
        }
    }
}

private const val PREFETCH_RADIUS = 2

private const val SETTLE_MS = 250L
