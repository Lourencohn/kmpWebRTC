package app.trovata.cast.data.auth

expect class AuthStore {
    fun isAuthenticated(): Boolean
    fun setAuthenticated(value: Boolean)
}
