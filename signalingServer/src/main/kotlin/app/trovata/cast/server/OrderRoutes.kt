package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.OrderSubmissionRequest
import app.trovata.cast.protocol.OrderSubmissionResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.orderRoutes(orders: OrderStore, sessions: SessionStore) {
    routing {
        route("/order") {
            post { call.submitOrder(orders, sessions) }
            get("/{orderId}") { call.fetchOrder(orders) }
        }
        route("/session/{token}/orders") {
            get { call.fetchOrdersForSession(orders) }
        }
    }
}

private suspend fun ApplicationCall.submitOrder(orders: OrderStore, sessions: SessionStore) {
    val request = try {
        receive<OrderSubmissionRequest>()
    } catch (_: Throwable) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body", "JSON inválido"))
        return
    }
    if (!OrderStore.isValid(request)) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_payload", "Pedido inválido"))
        return
    }
    if (sessions.get(request.sessionToken) == null) {
        respond(HttpStatusCode.NotFound, ErrorResponse("session_unknown", "Sessão não encontrada ou expirada"))
        return
    }
    val record = orders.submit(request)
    respond(
        HttpStatusCode.Accepted,
        OrderSubmissionResponse(orderId = record.orderId, receivedAtMs = record.receivedAtMs),
    )
}

private suspend fun ApplicationCall.fetchOrder(orders: OrderStore) {
    val orderId = parameters["orderId"].orEmpty()
    val record = orders.get(orderId)
    if (record == null) {
        respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Pedido não encontrado"))
        return
    }
    respond(record)
}

private suspend fun ApplicationCall.fetchOrdersForSession(orders: OrderStore) {
    val token = parameters["token"].orEmpty()
    respond(orders.byToken(token))
}
