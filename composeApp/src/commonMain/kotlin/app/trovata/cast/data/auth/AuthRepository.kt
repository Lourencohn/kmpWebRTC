package app.trovata.cast.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val store: AuthStore) {
    private val _isAuthenticated: MutableStateFlow<Boolean> = MutableStateFlow(store.isAuthenticated())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun signIn() {
        store.setAuthenticated(true)
        _isAuthenticated.value = true
    }

    fun signOut() {
        store.setAuthenticated(false)
        _isAuthenticated.value = false
    }
}
