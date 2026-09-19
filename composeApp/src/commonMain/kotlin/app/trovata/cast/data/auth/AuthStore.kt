package app.trovata.cast.data.auth

interface AuthStorage {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

expect class AuthStore : AuthStorage {
    override fun get(key: String): String?
    override fun put(key: String, value: String?)
}
