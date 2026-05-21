package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.sessionRoutes(store: SessionStore, baseUrl: String) {
    routing {
        route("/session") {
            post { call.createSession(store, baseUrl) }
            get("/{token}") { call.fetchSession(store) }
        }
    }
}

private suspend fun ApplicationCall.createSession(store: SessionStore, baseUrl: String) {
    val request = try {
        receive<SessionCreateRequest>()
    } catch (_: Throwable) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body", "JSON inválido"))
        return
    }
    if (request.productSkus.isEmpty()) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("empty_selection", "Selecione ao menos um produto"))
        return
    }
    val stored = store.create(request)
    respond(
        HttpStatusCode.Created,
        SessionCreateResponse(
            sessionId = stored.sessionId,
            token = stored.token,
            url = "$baseUrl/?t=${stored.token}",
            expiresAtMs = stored.expiresAtMs,
        ),
    )
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
