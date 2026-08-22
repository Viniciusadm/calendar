package com.archieapps.calendar.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.archieapps.calendar.design.components.TabItem
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.ListChecks
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SlidersHorizontal

enum class Tab(val label: String, val icon: ImageVector) {
    Tasks("tarefas", Lucide.ListChecks),
    Calendar("calendário", Lucide.CalendarDays),
    Settings("ajustes", Lucide.SlidersHorizontal);

    fun item(badge: Int? = null): TabItem = TabItem(label = label, icon = icon, badge = badge)
}

enum class Leaf { Root, Agenda, Categories }
