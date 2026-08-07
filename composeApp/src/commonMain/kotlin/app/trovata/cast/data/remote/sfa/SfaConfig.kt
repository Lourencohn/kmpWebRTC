package app.trovata.cast.data.remote.sfa

data class SfaCredentials(
    val username: String,
    val password: String,
)

object SfaConfig {
    const val baseUrl: String = "https://api-int.trovata.app.br"
    const val keycloakUrl: String = "https://login.trovata.app.br"
    const val realm: String = "Base"
    const val clientId: String = "front-client"
    const val empresaId: Long = 97L
    const val empresaSlug: String = ""
    const val catalogoLinkUuid: String = ""
}
