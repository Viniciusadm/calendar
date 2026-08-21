package com.archieapps.calendar.core.net

import com.archieapps.calendar.core.store.Settings
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T, val misc: FeedMisc?) : ApiResult<T>
    data class Failure(val message: String) : ApiResult<Nothing>
}

class CalendarApi(private val settings: Settings) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun occurrences(start: String, end: String, kinds: String? = null): ApiResult<List<OccurrenceDto>> =
        get("/api/calendar/events", buildMap {
            put("start", start)
            put("end", end)
            if (kinds != null) put("kinds", kinds)
        }) { json.decodeFromString<Envelope<List<OccurrenceDto>>>(it) }

    suspend fun categories(): ApiResult<List<CategoryDto>> =
        get("/api/calendar/categories", emptyMap()) { json.decodeFromString<Envelope<List<CategoryDto>>>(it) }

    private suspend fun <T> get(
        path: String,
        query: Map<String, String>,
        decode: (String) -> Envelope<T>,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val token = settings.token
        if (token.isNullOrBlank()) return@withContext ApiResult.Failure("Configure o token de acesso.")

        val base = (settings.baseUrl + path).toHttpUrlOrNull()
            ?: return@withContext ApiResult.Failure("Endereço do servidor inválido.")

        val url = base.newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .header("code", SafeCode.today())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()

                if (raw.isBlank()) {
                    return@use ApiResult.Failure("O servidor respondeu vazio (${response.code}).")
                }

                val envelope = decode(raw)
                val value = envelope.body

                when {
                    envelope.success && value != null -> ApiResult.Ok(value, envelope.misc)
                    else -> ApiResult.Failure(envelope.error ?: envelope.message ?: "Falha ao carregar (${response.code}).")
                }
            }
        } catch (error: IOException) {
            ApiResult.Failure("Sem conexão com o servidor.")
        } catch (error: Exception) {
            ApiResult.Failure(error.message ?: "Resposta inesperada do servidor.")
        }
    }
}
