package com.archieapps.calendar.core.net

import java.time.OffsetDateTime

object PrivateUnlock {
    const val HEADER = "X-Private-Unlock"

    @Volatile
    private var token: String? = null

    @Volatile
    private var expiresAtMillis: Long = 0

    val active: Boolean
        get() = token != null && System.currentTimeMillis() < expiresAtMillis

    fun header(): String? = token.takeIf { active }

    fun grant(token: String, expiresAt: String?) {
        this.token = token
        this.expiresAtMillis = parse(expiresAt)
    }

    fun clear() {
        token = null
        expiresAtMillis = 0
    }

    private fun parse(expiresAt: String?): Long {
        val parsed = expiresAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

        return parsed ?: (System.currentTimeMillis() + DEFAULT_TTL_MILLIS)
    }

    private const val DEFAULT_TTL_MILLIS = 30L * 60L * 1000L
}
