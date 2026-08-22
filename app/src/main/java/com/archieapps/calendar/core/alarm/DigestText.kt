package com.archieapps.calendar.core.alarm

data class DigestContent(val title: String, val body: String)

fun digestContent(
    titles: List<String>,
    pending: Int,
    overdue: Int,
    notifyOverdue: Boolean,
    maxTitles: Int = 4,
): DigestContent? {
    val late = if (notifyOverdue) overdue.coerceAtLeast(0) else 0
    val due = pending.coerceAtLeast(0)

    if (due == 0 && late == 0) {
        return null
    }

    val head = buildList {
        if (due > 0) add(if (due == 1) "1 tarefa para hoje" else "$due tarefas para hoje")
        if (late > 0) add(if (late == 1) "1 atrasada" else "$late atrasadas")
    }.joinToString(" · ")

    val shown = titles.take(maxTitles)
    val hidden = titles.size - shown.size

    val body = buildList {
        addAll(shown)
        if (hidden > 0) add("e mais $hidden")
    }.joinToString(" · ").ifBlank { "abra para ver o que ficou" }

    return DigestContent(title = head, body = body)
}
