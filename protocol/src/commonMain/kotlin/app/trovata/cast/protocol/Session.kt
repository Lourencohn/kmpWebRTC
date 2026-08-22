package app.trovata.cast.protocol

import kotlinx.serialization.Serializable

@Serializable
data class SessionCreateRequest(
    val empresaSlug: String,
    val catalogoUuid: String,
    val sellerId: String,
    val sellerName: String,
    val catalogoNome: String? = null,
    val carrinhoId: Long? = null,
    val clientName: String? = null,
    val clientEmail: String? = null,
)

@Serializable
data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
)

@Serializable
data class SessionCreateResponse(
    val sessionId: String,
    val token: String,
    val url: String,
    val expiresAtMs: Long,
    val iceServers: List<IceServerConfig> = emptyList(),
)

@Serializable
data class SessionInfo(
    val token: String,
    val empresaSlug: String,
    val catalogoUuid: String,
    val sellerName: String,
    val catalogoNome: String? = null,
    val carrinhoId: Long? = null,
    val clientName: String? = null,
    val clientEmail: String? = null,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val iceServers: List<IceServerConfig> = emptyList(),
)

@Serializable
data class ErrorResponse(val code: String, val message: String)

const val LiveSessionQueryParam = "live"

fun buildLiveInviteUrl(
    catalogBaseUrl: String,
    empresaSlug: String,
    catalogoUuid: String,
    token: String,
): String {
    val base = catalogBaseUrl.trimEnd('/')
    return "$base/catalogo-link-view/$empresaSlug/$catalogoUuid?$LiveSessionQueryParam=$token"
}
