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
    val expandRecurringTasks: Boolean = false,
    val dayFill: Map<LocalDate, List<CalendarEntry>> = emptyMap(),
) {
    fun entriesFor(month: YearMonth): Map<LocalDate, List<CalendarEntry>> = months[month].orEmpty()

    val selectedEntries: List<CalendarEntry>
        get() {
            val grid = entriesFor(month)[selected].orEmpty()
            val extra = dayFill[selected].orEmpty()

            if (extra.isEmpty()) {
                return grid
            }

            val known = grid.mapTo(mutableSetOf()) { it.id }

            return (grid + extra.filterNot { known.contains(it.id) })
                .sortedWith(compareBy({ it.allDay.not() }, { it.clock ?: "" }, { it.title }))
        }
}

fun LocalDate.carriedInto(month: YearMonth): LocalDate {
    val today = LocalDate.now()

    if (month == YearMonth.from(today)) return today

    return month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
}

fun YearMonth.gridStart(): LocalDate =
    atDay(1).with(TemporalAdjusters.previousOrSame(WeekStart.firstDay))

fun YearMonth.gridDays(): List<LocalDate> {
    val start = gridStart()

    return (0 until 42).map { start.plusDays(it.toLong()) }
}

class CalendarViewModel(private val api: CalendarApi) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var loadJob: Job? = null

    private var detailJob: Job? = null

    private var dayJob: Job? = null

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

    fun select(date: LocalDate) {
        _state.update { it.copy(selected = date) }
        fillDay(date)
    }

    fun setExpandRecurringTasks(expand: Boolean) {
        if (_state.value.expandRecurringTasks == expand) {
            return
        }

        _state.update { it.copy(expandRecurringTasks = expand, dayFill = emptyMap()) }
        ensure(_state.value.month, force = true, settle = false)
    }

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

        return when (
            val result = api.occurrences(
                start = start.toString(),
                end = end.toString(),
                recurringTasks = recurringTaskMode(),
            )
        ) {
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
                        dayFill = emptyMap(),
                        loading = if (visible) false else current.loading,
                        error = if (visible) null else current.error,
                    )
                }

                fillDay(_state.value.selected)

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

    private fun recurringTaskMode(): String? =
        if (_state.value.expandRecurringTasks) null else "today"

    private fun fillDay(date: LocalDate) {
        val snapshot = _state.value

        if (snapshot.expandRecurringTasks || date == LocalDate.now() || snapshot.dayFill.containsKey(date)) {
            return
        }

        dayJob?.cancel()
        dayJob = viewModelScope.launch {
            delay(DAY_SETTLE_MS)

            val result = api.occurrences(
                start = date.toString(),
                end = date.toString(),
                natures = "task",
                recurringTasks = "window",
            )

            if (result !is ApiResult.Ok) {
                return@launch
            }

            val recurring = result.value
                .filter { it.recurring && it.nature == "task" }
                .map { it.toEntry() }

            _state.update { current ->
                if (current.dayFill.containsKey(date)) {
                    current
                } else {
                    current.copy(dayFill = current.dayFill + (date to recurring))
                }
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

        if (entry == null) {
            _state.update { it.copy(focused = null, focusedDetail = DetailState.Absent) }

            return
        }

        _state.update { it.copy(focused = entry, focusedDetail = DetailState.Loading) }

        detailJob = viewModelScope.launch {
            val detail = load(entry)

            if (_state.value.focused?.id != entry.id) return@launch

            _state.update { it.copy(focusedDetail = detail) }
        }
    }

    private suspend fun load(entry: CalendarEntry): DetailState {
        val eventId = entry.eventId

        val detail = if (eventId == null) {
            when (val result = api.occurrenceDetail(entry.id)) {
                is ApiResult.Ok -> result.value.toDetail(entry.date)
                is ApiResult.Failure -> null
            }
        } else {
            when (val result = api.event(eventId)) {
                is ApiResult.Ok -> result.value.toDetail()
                is ApiResult.Failure -> null
            }
        }

        return detail?.let { DetailState.Ready(it) } ?: DetailState.Absent
    }

    fun newEvent() {
        openDraft(isTask = false)
    }

    fun newTask() {
        openDraft(isTask = true)
    }

    private fun openDraft(isTask: Boolean) {
        val snapshot = _state.value
        val fallback = snapshot.categories.firstOrNull { it.isDefault }?.id
        val anchor = if (isTask) LocalDate.now() else snapshot.selected

        _state.update {
            it.copy(
                draft = EventDraft.forDay(anchor, fallback).copy(isTask = isTask),
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

            val message = when {
                draft.isTask && draft.isEditing -> "Tarefa atualizada."
                draft.isTask -> "Tarefa criada."
                draft.isEditing -> "Evento atualizado."
                else -> "Evento criado."
            }

            finish(result, message) {
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

private const val DAY_SETTLE_MS = 220L

private const val PREFETCH_RADIUS = 2

private const val SETTLE_MS = 250L
