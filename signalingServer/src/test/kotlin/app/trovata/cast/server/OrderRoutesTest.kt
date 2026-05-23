package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.OrderLine
import app.trovata.cast.protocol.OrderRecord
import app.trovata.cast.protocol.OrderSource
import app.trovata.cast.protocol.OrderSubmissionRequest
import app.trovata.cast.protocol.OrderSubmissionResponse
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderRoutesTest {

    private fun sampleSession() = SessionCreateRequest(
        sellerId = "atelier-norte",
        sellerName = "Atelier Norte",
        collectionLabel = "Outono 26",
        productSkus = listOf("AN-104"),
        clientName = "Diego",
        clientShop = "Trama",
        scheduledFor = null,
    )

    private fun sampleOrder(token: String) = OrderSubmissionRequest(
        orderId = "ORD-test-1",
        sessionToken = token,
        tsMs = 1_700_000_000_000,
        totalCents = 17_980,
        lines = listOf(
            OrderLine(productId = "AN-104", size = "M", units = 2, unitPriceCents = 8_990),
        ),
        source = OrderSource.Buyer,
        clientName = "Diego",
    )

    @Test
    fun submitAndFetchOrder() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleSession())
        }.body()

        val submission = sampleOrder(created.token)
        val submitResponse = client.post("/order") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }
        assertEquals(HttpStatusCode.Accepted, submitResponse.status)
        val ack: OrderSubmissionResponse = submitResponse.body()
        assertEquals(submission.orderId, ack.orderId)
        assertTrue(ack.receivedAtMs > 0)

        val fetched: OrderRecord = client.get("/order/${submission.orderId}").body()
        assertEquals(submission.orderId, fetched.orderId)
        assertEquals(submission.totalCents, fetched.totalCents)
        assertEquals(1, fetched.lines.size)
        assertEquals(OrderSource.Buyer, fetched.source)
    }

    @Test
    fun submitIsIdempotent() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleSession())
        }.body()

        val submission = sampleOrder(created.token)
        client.post("/order") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }
        val second = client.post("/order") {
            contentType(ContentType.Application.Json)
            setBody(submission.copy(totalCents = 999_999))
        }
        assertEquals(HttpStatusCode.Accepted, second.status)
        val fetched: OrderRecord = client.get("/order/${submission.orderId}").body()
        assertEquals(submission.totalCents, fetched.totalCents)
    }

    @Test
    fun submitWithUnknownTokenIs404() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.post("/order") {
            contentType(ContentType.Application.Json)
            setBody(sampleOrder("zzzzzzzzz"))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        val err: ErrorResponse = response.body()
        assertEquals("session_unknown", err.code)
    }

    @Test
    fun submitWithEmptyLinesIs400() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleSession())
        }.body()

        val response = client.post("/order") {
            contentType(ContentType.Application.Json)
            setBody(sampleOrder(created.token).copy(lines = emptyList()))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val err: ErrorResponse = response.body()
        assertEquals("invalid_payload", err.code)
    }

    @Test
    fun listOrdersForToken() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleSession())
        }.body()

        val submission = sampleOrder(created.token)
        client.post("/order") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }

        val list: List<OrderRecord> = client.get("/session/${created.token}/orders").body()
        assertEquals(1, list.size)
        assertEquals(submission.orderId, list.first().orderId)
    }
}
