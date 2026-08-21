package com.archieapps.calendar.core.net

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    pairs.forEach { (key, value) ->
        when (value) {
            null -> {}
            is String -> put(key, value)
            is Int -> put(key, value)
            is Boolean -> put(key, value)
            is JsonObject -> put(key, value)
            else -> put(key, JsonPrimitive(value.toString()))
        }
    }
}
