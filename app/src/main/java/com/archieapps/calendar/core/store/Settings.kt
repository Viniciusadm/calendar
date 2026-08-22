package com.archieapps.calendar.core.store

import android.content.Context
import android.content.SharedPreferences
import com.archieapps.calendar.BuildConfig

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronicle", Context.MODE_PRIVATE)

    val baseUrl: String = BuildConfig.BASE_URL.trimEnd('/')

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER, null)
        set(value) = prefs.edit().putString(KEY_USER, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var userImage: String?
        get() = prefs.getString(KEY_IMAGE, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_IMAGE, value?.takeIf { it.isNotBlank() }).apply()

    var revision: String?
        get() = prefs.getString(KEY_REVISION, null)
        set(value) = prefs.edit().putString(KEY_REVISION, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var weekStartsMonday: Boolean
        get() = prefs.getBoolean(KEY_WEEK_START, false)
        set(value) = prefs.edit().putBoolean(KEY_WEEK_START, value).apply()

    var initialTaskFilter: String
        get() = prefs.getString(KEY_TASK_FILTER, "pending") ?: "pending"
        set(value) = prefs.edit().putString(KEY_TASK_FILTER, value).apply()

    var expandRecurringInGrid: Boolean
        get() = prefs.getBoolean(KEY_EXPAND_RECURRING, false)
        set(value) = prefs.edit().putBoolean(KEY_EXPAND_RECURRING, value).apply()

    var digestEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGEST, false)
        set(value) = prefs.edit().putBoolean(KEY_DIGEST, value).apply()

    var digestMinuteOfDay: Int
        get() = prefs.getInt(KEY_DIGEST_AT, DEFAULT_DIGEST_AT)
        set(value) = prefs.edit().putInt(KEY_DIGEST_AT, value.coerceIn(0, 24 * 60 - 1)).apply()

    var notifyOverdue: Boolean
        get() = prefs.getBoolean(KEY_OVERDUE, true)
        set(value) = prefs.edit().putBoolean(KEY_OVERDUE, value).apply()

    val isLoggedIn: Boolean
        get() = token != null

    fun clearSession() = prefs.edit()
        .remove(KEY_TOKEN)
        .remove(KEY_USER)
        .remove(KEY_EMAIL)
        .remove(KEY_IMAGE)
        .remove(KEY_REVISION)
        .apply()

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val DEFAULT_DIGEST_AT = 8 * 60

        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_IMAGE = "user_image"
        private const val KEY_REVISION = "revision"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_WEEK_START = "week_starts_monday"
        private const val KEY_TASK_FILTER = "initial_task_filter"
        private const val KEY_EXPAND_RECURRING = "expand_recurring_grid"
        private const val KEY_DIGEST = "digest_enabled"
        private const val KEY_DIGEST_AT = "digest_minute_of_day"
        private const val KEY_OVERDUE = "notify_overdue"
    }
}
