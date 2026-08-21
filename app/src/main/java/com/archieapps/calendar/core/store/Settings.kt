package com.archieapps.calendar.core.store

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronicle", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trimEnd('/')).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var revision: String?
        get() = prefs.getString(KEY_REVISION, null)
        set(value) = prefs.edit().putString(KEY_REVISION, value).apply()

    val isConfigured: Boolean
        get() = !token.isNullOrBlank()

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_REVISION = "revision"
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8001"
    }
}
