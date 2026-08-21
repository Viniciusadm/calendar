package com.archieapps.calendar.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archieapps.calendar.core.net.ApiResult
import com.archieapps.calendar.core.net.CalendarApi
import com.archieapps.calendar.core.store.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !loading
}

class LoginViewModel(
    private val api: CalendarApi,
    private val settings: Settings,
    private val onAuthenticated: () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = api.login(current.email, current.password)) {
                is ApiResult.Ok -> {
                    settings.token = result.value.token
                    settings.userName = result.value.user?.name
                    settings.userEmail = result.value.user?.email
                    settings.userImage = result.value.user?.image
                    _state.update { it.copy(loading = false, password = "") }
                    onAuthenticated()
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
