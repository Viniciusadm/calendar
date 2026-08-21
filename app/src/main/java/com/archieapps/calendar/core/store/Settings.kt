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

    var revision: String?
        get() = prefs.getString(KEY_REVISION, null)
        set(value) = prefs.edit().putString(KEY_REVISION, value).apply()

    val isLoggedIn: Boolean
        get() = token != null

    fun clearSession() = prefs.edit()
        .remove(KEY_TOKEN)
        .remove(KEY_USER)
        .remove(KEY_REVISION)
        .apply()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user_name"
        const val KEY_REVISION = "revision"
    }
}
