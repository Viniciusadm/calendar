package com.archieapps.calendar.feature.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

object WeekStart {
    var firstDay by mutableStateOf(DayOfWeek.SUNDAY)

    fun apply(startsMonday: Boolean) {
        firstDay = if (startsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    }

    fun order(): List<DayOfWeek> = (0L..6L).map { firstDay.plus(it) }

    fun initials(): List<String> = order().map {
        it.getDisplayName(TextStyle.NARROW, ptBr).uppercase(ptBr).take(1)
    }
}
