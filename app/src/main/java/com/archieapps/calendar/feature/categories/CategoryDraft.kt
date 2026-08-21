package com.archieapps.calendar.feature.categories

import com.archieapps.calendar.core.net.CategoryDto
import com.archieapps.calendar.design.paletteTokens
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")

data class CategoryDraft(
    val id: Int? = null,
    val name: String = "",
    val emoji: String = "",
    val color: String = "primary",
    val defaultPriority: String = "none",
) {
    val isEditing: Boolean get() = id != null

    val isCustomColor: Boolean get() = color.startsWith("#")

    val canSave: Boolean get() = name.isNotBlank() && colorIsValid

    private val colorIsValid: Boolean
        get() = paletteTokens.any { it.token == color } || hexPattern.matches(color)

    fun withEmoji(value: String): CategoryDraft = copy(emoji = firstEmoji(value))

    fun withToken(token: String): CategoryDraft = copy(color = token)

    fun withHex(value: String): CategoryDraft {
        val typed = value.trim().take(7)

        return copy(color = if (typed.isEmpty()) "primary" else if (typed.startsWith("#")) typed else "#$typed")
    }

    fun toPayload(): JsonObject = buildJsonObject {
        put("name", name.trim())
        put("color", color.trim())
        put("defaultPriority", defaultPriority)

        if (emoji.isEmpty()) put("icon", JsonNull) else put("icon", emoji)
    }

    companion object {
        fun from(dto: CategoryDto): CategoryDraft = CategoryDraft(
            id = dto.id,
            name = dto.name,
            emoji = dto.emoji.orEmpty(),
            color = dto.colorToken ?: dto.color,
            defaultPriority = dto.defaultPriority,
        )
    }
}
