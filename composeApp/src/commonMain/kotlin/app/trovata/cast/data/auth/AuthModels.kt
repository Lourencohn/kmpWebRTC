package app.trovata.cast.data.auth

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtMs: Long,
    val refreshExpiresAtMs: Long,
)

data class AuthUser(
    val id: Long,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val whatsapp: String?,
)

data class Company(
    val id: Long,
    val name: String,
    val legalName: String?,
    val cnpj: String?,
    val logoUrl: String?,
)

sealed class LoginResult {
    data class Success(val needsCompanySelection: Boolean) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}
