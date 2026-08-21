package com.archieapps.calendar.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
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
    val loading: Boolean = false,
    val error: String? = null,
) {
    val gridStart: LocalDate
        get() = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    val gridDays: List<LocalDate>
        get() = (0 until 42).map { gridStart.plusDays(it.toLong()) }

    fun entriesOn(date: LocalDate): List<CalendarEntry> = entries[date].orEmpty()

    val selectedEntries: List<CalendarEntry> get() = entriesOn(selected)
}

class CalendarViewModel(private val api: CalendarApi) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        load()
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

    fun load() {
        val snapshot = _state.value
        val start = snapshot.gridStart
        val end = start.plusDays(41)

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = api.occurrences(start.toString(), end.toString())) {
                is ApiResult.Ok -> {
                    val grouped = result.value.map { it.toEntry() }.groupBy { it.date }
                    _state.update { it.copy(entries = grouped, loading = false, error = null) }
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
