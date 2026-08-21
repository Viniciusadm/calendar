package com.archieapps.calendar.feature.categories

import android.icu.text.BreakIterator
import com.archieapps.calendar.core.net.CategoryDto

val priorityChoices: List<Pair<String, String>> = listOf(
    "none" to "nenhuma",
    "low" to "baixa",
    "medium" to "média",
    "high" to "alta",
)

val CategoryDto.emoji: String?
    get() = icon?.trim()?.takeIf { it.isNotEmpty() && it.none { char -> char.code < 128 } }

fun CategoryDto.pillLabel(): String = emoji?.let { "$it $name" } ?: name

fun CategoryDto.priorityLabel(): String =
    priorityChoices.firstOrNull { it.first == defaultPriority }?.second ?: defaultPriority

fun firstEmoji(value: String): String {
    val grapheme = firstGrapheme(value)

    return if (grapheme.none { it.code < 128 }) grapheme else ""
}

private fun firstGrapheme(value: String): String {
    val trimmed = value.trim()

    if (trimmed.isEmpty()) return ""

    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(trimmed)

    val end = iterator.next()

    return if (end == BreakIterator.DONE) trimmed else trimmed.substring(0, end)
}

fun CategoryDto.linkSummary(): String? {
    val events = eventCount ?: 0
    val goals = goalCount ?: 0

    val parts = buildList {
        if (events > 0) add(if (events == 1) "1 evento" else "$events eventos")
        if (goals > 0) add(if (goals == 1) "1 meta" else "$goals metas")
    }

    return parts.takeIf { it.isNotEmpty() }?.joinToString(" e ")
}

fun CategoryDto.metaLine(): String = buildList {
    if (isDefault) add("padrão")
    if (!active) add("arquivada")
    if (defaultPriority != "none") add("prioridade ${priorityLabel()}")
    linkSummary()?.let { add(it) }
}.joinToString(" · ")
