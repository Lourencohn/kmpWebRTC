package app.trovata.cast.data.auth

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtMs: Long,
    val refreshExpiresAtMs: Long,
) {
    companion object {
        const val NEVER_EXPIRES: Long = Long.MAX_VALUE
    }
}

data class AuthUser(
    val id: Long,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val whatsapp: String?,
)

data class Company(
    val id: Long,
    val slug: String,
    val name: String,
    val legalName: String?,
    val cnpj: String?,
    val logoUrl: String?,
)

sealed class LoginResult {
    data class Success(val needsCompanySelection: Boolean) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}

data class SellerIdentity(val id: String, val name: String)

fun sellerIdentityOf(user: AuthUser?, company: Company?): SellerIdentity = SellerIdentity(
    id = (user?.id ?: company?.id)?.toString() ?: "seller",
    name = user?.name?.takeIf { it.isNotBlank() } ?: company?.name ?: "Vendedor",
)
