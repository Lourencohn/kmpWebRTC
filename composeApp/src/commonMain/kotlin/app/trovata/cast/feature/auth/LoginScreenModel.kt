package app.trovata.cast.feature.auth

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.LoginResult
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: LoginResult.Success? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

class LoginScreenModel(
    private val authRepository: AuthRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }

    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val snapshot = _state.value
        if (!snapshot.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        screenModelScope.launch {
            when (val result = authRepository.login(snapshot.email, snapshot.password)) {
                is LoginResult.Success -> _state.update { it.copy(isSubmitting = false, success = result) }
                is LoginResult.Failure -> _state.update { it.copy(isSubmitting = false, error = result.message) }
            }
        }
    }
}
