package com.archieapps.calendar.core.store

import android.content.Context
import android.content.SharedPreferences
import com.archieapps.calendar.BuildConfig

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronicle", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = (prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL).trimEnd('/')
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

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
        val DEFAULT_BASE_URL: String = BuildConfig.BASE_URL.trimEnd('/')
    }
}
