package com.lipapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lipapp.data.prefs.AppPreferences
import com.lipapp.data.repository.LipserviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isInitialized: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: LipserviceRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    init {
        viewModelScope.launch {
            val url = prefs.url.first()
            val username = prefs.username.first()
            _state.value = _state.value.copy(
                url = url,
                username = username,
                isInitialized = true,
            )
        }
    }

    fun updateUrl(url: String) {
        _state.value = _state.value.copy(url = url, error = null)
    }

    fun updateUsername(username: String) {
        _state.value = _state.value.copy(username = username, error = null)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.url.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "All fields are required")
            return
        }

        _state.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                repository.login(s.url, s.username, s.password)
                _state.value = _state.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("401") == true -> "Invalid credentials"
                    e.message?.contains("ConnectException") == true -> "Cannot reach server"
                    else -> e.message ?: "Login failed"
                }
                _state.value = _state.value.copy(isLoading = false, error = msg)
            }
        }
    }
}
