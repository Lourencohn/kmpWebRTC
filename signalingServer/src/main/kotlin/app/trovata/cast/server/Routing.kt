package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import app.trovata.cast.protocol.buildLiveInviteUrl
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.sessionRoutes(store: SessionStore, catalogBaseUrl: String) {
    routing {
        route("/session") {
            post { call.createSession(store, catalogBaseUrl) }
            get("/{token}") { call.fetchSession(store) }
        }
    }
}

private suspend fun ApplicationCall.createSession(store: SessionStore, catalogBaseUrl: String) {
    val request = try {
        receive<SessionCreateRequest>()
    } catch (_: Throwable) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body", "JSON inválido"))
        return
    }
    val invalid = request.validationError()
    if (invalid != null) {
        respond(HttpStatusCode.BadRequest, invalid)
        return
    }
    val stored = store.create(request)
    respond(
        HttpStatusCode.Created,
        SessionCreateResponse(
            sessionId = stored.sessionId,
            token = stored.token,
            url = buildLiveInviteUrl(
                catalogBaseUrl = catalogBaseUrl,
                empresaSlug = request.empresaSlug,
                catalogoUuid = request.catalogoUuid,
                token = stored.token,
            ),
            expiresAtMs = stored.expiresAtMs,
        ),
    )
}

private fun SessionCreateRequest.validationError(): ErrorResponse? = when {
    empresaSlug.isBlank() -> ErrorResponse("missing_empresa", "Informe a empresa do catálogo")
    catalogoUuid.isBlank() -> ErrorResponse("missing_catalogo", "Informe o catálogo link da sessão")
    sellerId.isBlank() -> ErrorResponse("missing_seller", "Informe o vendedor da sessão")
    else -> null
}

private suspend fun ApplicationCall.fetchSession(store: SessionStore) {
    val token = parameters["token"].orEmpty()
    val stored = store.get(token)
    if (stored == null) {
        respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Sessão não encontrada ou expirada"))
        return
    }
    respond(stored.toInfo())
}
