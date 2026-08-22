package com.archieapps.calendar.ui

import com.archieapps.calendar.design.components.TabGlyph
import com.archieapps.calendar.design.components.TabItem

enum class Tab(val label: String, val glyph: TabGlyph) {
    Tasks("tarefas", TabGlyph.List),
    Calendar("calendário", TabGlyph.Grid),
    Settings("ajustes", TabGlyph.Sliders);

    fun item(badge: Int? = null): TabItem = TabItem(label = label, glyph = glyph, badge = badge)
}

enum class Leaf { Root, Agenda, Categories }
