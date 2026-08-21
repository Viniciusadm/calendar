package com.archieapps.calendar.core.net

import android.util.Log
import com.archieapps.calendar.core.store.Settings
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T, val misc: FeedMisc? = null) : ApiResult<T>
    data class Failure(
        val message: String,
        val unauthorized: Boolean = false,
        val gated: Boolean = false,
    ) : ApiResult<Nothing>
}

class CalendarApi(private val settings: Settings) {
    private val client = Http.shared

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun occurrences(start: String, end: String, kinds: String? = null): ApiResult<List<OccurrenceDto>> =
        send("GET", "/api/calendar/events", buildMap {
            put("start", start)
            put("end", end)
            if (kinds != null) put("kinds", kinds)
        }) { json.decodeFromString<Envelope<List<OccurrenceDto>>>(it).let { e -> e.success to (e.body to e.misc) } }

    suspend fun categories(): ApiResult<List<CategoryDto>> =
        send("GET", "/api/calendar/categories", emptyMap()) {
            json.decodeFromString<Envelope<List<CategoryDto>>>(it).let { e -> e.success to (e.body to e.misc) }
        }

    suspend fun event(id: Int): ApiResult<EventDto> =
        send("GET", "/api/calendar/events/$id", emptyMap()) {
            json.decodeFromString<Envelope<EventDto>>(it).let { e -> e.success to (e.body to e.misc) }
        }

    suspend fun login(email: String, password: String): ApiResult<Session> =
        send(
            method = "POST",
            path = "/api/auth/login",
            query = emptyMap(),
            payload = jsonOf("email" to email.trim(), "password" to password),
            authenticated = false,
        ) { json.decodeFromString<Envelope<Session>>(it).let { e -> e.success to (e.body to e.misc) } }

    suspend fun validateCode(code: String): ApiResult<Unit> =
        send("POST", "/api/user/test", emptyMap(), jsonOf("code" to code)) {
            json.decodeFromString<Ack>(it).let { ack -> ack.success to (Unit to null) }
        }

    suspend fun profile(): ApiResult<SessionUser> =
        send("GET", "/api/user", emptyMap()) { raw ->
            val user = runCatching { json.decodeFromString<Envelope<SessionUser>>(raw).body }.getOrNull()
                ?: runCatching { json.decodeFromString<SessionUser>(raw) }.getOrNull()

            (user?.id != null) to (user to null)
        }

    suspend fun syncState(): ApiResult<SyncStateDto> =
        send("GET", "/api/calendar/sync/state", emptyMap()) {
            json.decodeFromString<Envelope<SyncStateDto>>(it).let { e -> e.success to (e.body to e.misc) }
        }

    suspend fun reminderSchedule(from: String, days: Int): ApiResult<List<ReminderPlanEntry>> =
        send("GET", "/api/calendar/reminders/schedule", mapOf("from" to from, "days" to days.toString())) {
            json.decodeFromString<Envelope<List<ReminderPlanEntry>>>(it).let { e -> e.success to (e.body to e.misc) }
        }

    suspend fun createEvent(payload: JsonObject): ApiResult<Unit> =
        mutate("POST", "/api/calendar/events", payload)

    suspend fun updateEvent(id: Int, payload: JsonObject): ApiResult<Unit> =
        mutate("PUT", "/api/calendar/events/$id", payload)

    suspend fun deleteEvent(id: Int): ApiResult<Unit> =
        mutate("DELETE", "/api/calendar/events/$id", null)

    suspend fun setCompletion(occurrenceId: String, completed: Boolean): ApiResult<Unit> =
        mutate("PATCH", "/api/calendar/occurrences/$occurrenceId/completion", jsonOf("completed" to completed))

    suspend fun cancelOccurrence(occurrenceId: String): ApiResult<Unit> =
        mutate("DELETE", "/api/calendar/occurrences/$occurrenceId", null)

    private suspend fun mutate(method: String, path: String, payload: JsonObject?): ApiResult<Unit> =
        send(method, path, emptyMap(), payload) {
            json.decodeFromString<Ack>(it).let { ack ->
                ack.success to (Unit to null)
            }
        }.let { result ->
            when (result) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Failure -> result
            }
        }

    private suspend fun <T> send(
        method: String,
        path: String,
        query: Map<String, String>,
        payload: JsonObject? = null,
        authenticated: Boolean = true,
        decode: (String) -> Pair<Boolean, Pair<T?, FeedMisc?>>,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val token = settings.token

        if (authenticated && token == null) {
            return@withContext ApiResult.Failure("Sessão expirada. Entre de novo.", unauthorized = true)
        }

        val base = (settings.baseUrl + path).toHttpUrlOrNull()
            ?: return@withContext ApiResult.Failure("Endereço do servidor inválido.")

        val url = base.newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()

        val body: RequestBody? = when {
            payload != null -> payload.toString().toRequestBody(jsonMedia)
            method == "POST" || method == "PUT" || method == "PATCH" -> "{}".toRequestBody(jsonMedia)
            else -> null
        }

        val request = Request.Builder()
            .url(url)
            .method(method, body)
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .apply { AccessCode.header()?.let { header(AccessCode.HEADER, it) } }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()

                if (response.code == 401) {
                    return@use ApiResult.Failure("Sessão expirada. Entre de novo.", unauthorized = true)
                }

                if (raw.isBlank()) {
                    return@use ApiResult.Failure("O servidor respondeu vazio (${response.code}).")
                }

                val (success, payloadAndMisc) = decode(raw)
                val (value, misc) = payloadAndMisc

                if (success && value != null) {
                    ApiResult.Ok(value, misc)
                } else {
                    ApiResult.Failure(errorFrom(raw, response.code), gated = isGated(response.code, raw))
                }
            }
        } catch (error: IOException) {
            ApiResult.Failure("Sem conexão com o servidor.")
        } catch (error: Exception) {
            Log.w(TAG, "falha em $method $path", error)
            ApiResult.Failure(error.message ?: "Resposta inesperada do servidor.")
        }
    }

    private fun isGated(status: Int, raw: String): Boolean =
        status == 400 && raw.contains(GATE_MESSAGE)

    private fun errorFrom(raw: String, code: Int): String {
        val ack = runCatching { json.decodeFromString<Ack>(raw) }.getOrNull()

        Log.w(TAG, "resposta inesperada ($code): ${raw.take(240)}")

        return ack?.error ?: ack?.message ?: "Falha na requisição ($code)."
    }

    private companion object {
        const val TAG = "ChronicleApi"

        const val GATE_MESSAGE = "digo inv"
    }
}
