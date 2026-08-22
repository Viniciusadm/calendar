package com.archieapps.calendar.core.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WidgetRevision {
    private val _value = MutableStateFlow(0)

    private val _fromWidget = MutableStateFlow(0)

    val value: StateFlow<Int> = _value.asStateFlow()

    val fromWidget: StateFlow<Int> = _fromWidget.asStateFlow()

    fun bump() {
        _value.value = _value.value + 1
    }

    fun bumpFromWidget() {
        _fromWidget.value = _fromWidget.value + 1
        bump()
    }
}
