package com.archieapps.calendar.feature.calendar

import com.archieapps.calendar.core.net.EventDto
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

val reminderChoices: List<Pair<String, Int?>> = listOf(
    "sem lembrete" to null,
    "na hora" to 0,
    "15 min antes" to 15,
    "1 h antes" to 60,
    "1 dia antes" to 1440,
)

data class EventDraft(
    val eventId: Int? = null,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val allDay: Boolean = true,
    val time: String = "09:00",
    val durationMinutes: Int = 60,
    val categoryId: Int? = null,
    val priority: String = "none",
    val isTask: Boolean = false,
    val recurrence: Recurrence = Recurrence(),
    val reminderMinutes: Int? = null,
    val days: Int = 1,
) {
    val isEditing: Boolean get() = eventId != null

    val canSave: Boolean get() = title.isNotBlank()

    fun onDate(date: LocalDate): EventDraft = copy(date = date, recurrence = recurrence.normalized(date))

    fun withRecurrence(transform: (Recurrence) -> Recurrence): EventDraft =
        copy(recurrence = transform(recurrence).normalized(date))

    fun withUnit(unit: RepeatUnit?): EventDraft = withRecurrence { rule ->
        rule.copy(
            unit = unit,
            weekdays = if (unit == RepeatUnit.Week && rule.weekdays.isEmpty()) {
                setOf(date.dayOfWeek)
            } else {
                rule.weekdays
            },
        )
    }

    fun toPayload(): JsonObject = buildJsonObject {
        put("title", title.trim())
        put("nature", if (isTask) "task" else "event")
        put("date", date.toString())
        put("allDay", allDay)
        if (allDay) {
            put("durationMinutes", days.coerceAtLeast(1) * 1440)
        } else {
            put("time", time)
            put("durationMinutes", durationMinutes.coerceAtLeast(1))
        }
        put("priority", priority)
        categoryId?.let { put("categoryId", it) }
        put("recurrence", recurrence.rule(date))
        putJsonArray("reminders") {
            reminderMinutes?.let { minutes ->
                add(buildJsonObject { put("minutesBefore", minutes) })
            }
        }
    }

    companion object {
        fun forDay(date: LocalDate, defaultCategory: Int?): EventDraft =
            EventDraft(date = date, categoryId = defaultCategory)

        fun from(dto: EventDto): EventDraft = EventDraft(
            eventId = dto.id,
            title = dto.title,
            date = LocalDate.parse(dto.date),
            allDay = dto.allDay,
            time = dto.time ?: "09:00",
            durationMinutes = if (dto.allDay) 60 else dto.durationMinutes,
            categoryId = dto.categoryId,
            priority = dto.priority,
            isTask = dto.nature == "task",
            recurrence = Recurrence.parse(dto.recurrence),
            reminderMinutes = dto.reminders.firstOrNull { it.active }?.minutesBefore,
            days = if (dto.allDay) (dto.durationMinutes / 1440).coerceAtLeast(1) else 1,
        )
    }
}
