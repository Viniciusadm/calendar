package com.archieapps.calendar.core.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Envelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    val body: T? = null,
    val misc: FeedMisc? = null,
    val error: String? = null,
)

@Serializable
data class FeedMisc(
    val window: FeedWindow? = null,
    val truncated: Boolean = false,
)

@Serializable
data class FeedWindow(val start: String? = null, val end: String? = null)

@Serializable
data class OccurrenceDto(
    val id: String,
    val kind: String,
    val nature: String,
    val eventId: Int? = null,
    val sourceId: Int? = null,
    val seriesDate: String,
    val date: String,
    val endDate: String,
    val time: String? = null,
    val endTime: String? = null,
    val allDay: Boolean = true,
    val durationMinutes: Int = 1440,
    val startsAt: String,
    val endsAt: String,
    val timezone: String,
    val title: String,
    val description: String? = null,
    val color: String,
    val colorToken: String? = null,
    val colorSource: String,
    val priority: String = "none",
    val priorityWeight: Int = 0,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val recurring: Boolean = false,
    val overridden: Boolean = false,
    val completed: Boolean = false,
    val completedAt: String? = null,
    val editable: Boolean = false,
    val requiresCode: Boolean = false,
    val remindersMuted: Boolean = false,
    val meta: JsonObject? = null,
)

@Serializable
data class Ack(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class EventDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val nature: String = "event",
    val date: String,
    val time: String? = null,
    val allDay: Boolean = true,
    val durationMinutes: Int = 1440,
    val categoryId: Int? = null,
    val priority: String = "none",
    val color: String? = null,
    val colorToken: String? = null,
    val recurrence: String? = null,
    val recurrenceEndsAt: String? = null,
    val requiresCode: Boolean = false,
    val reminders: List<ReminderDto> = emptyList(),
    val items: List<EventItemDto> = emptyList(),
    val nextOccurrences: List<SeriesDateDto> = emptyList(),
)

@Serializable
data class SeriesDateDto(
    val seriesDate: String,
    val date: String,
    val time: String? = null,
    val allDay: Boolean = true,
)

@Serializable
data class EventItemDto(
    val id: Int,
    val title: String,
    val durationMinutes: Int? = null,
    val position: Int = 0,
)

@Serializable
data class ReminderDto(
    val id: Int = 0,
    val minutesBefore: Int,
    val method: String = "device",
    val label: String? = null,
    val active: Boolean = true,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class Session(
    val user: SessionUser? = null,
    val token: String,
    val type: String = "Bearer",
)

@Serializable
data class SessionUser(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val image: String? = null,
)

@Serializable
data class SyncStateDto(
    val revision: String,
    val domains: Map<String, String> = emptyMap(),
    val calculatedAt: String? = null,
)

@Serializable
data class ReminderPlanEntry(
    val key: String,
    val reminderId: Int,
    val eventId: Int,
    val occurrenceId: String,
    val triggerAt: String,
    val occurrenceStartAt: String,
    val minutesBefore: Int,
    val allDay: Boolean = false,
    val title: String,
    val label: String? = null,
    val categoryName: String? = null,
)

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String,
    val slug: String,
    val color: String,
    val colorToken: String? = null,
    val icon: String? = null,
    @SerialName("defaultPriority") val defaultPriority: String = "none",
    val active: Boolean = true,
    @SerialName("isDefault") val isDefault: Boolean = false,
    val position: Int = 0,
    val eventCount: Int? = null,
    val goalCount: Int? = null,
)
