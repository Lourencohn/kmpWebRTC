package app.trovata.cast.data.auth

expect class AuthStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}
