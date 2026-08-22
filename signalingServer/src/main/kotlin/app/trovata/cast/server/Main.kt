package app.trovata.cast.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowHeader("Authorization")
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Options)
    }
    install(WebSockets) {
        pingPeriodMillis = 20_000
        timeoutMillis = 30_000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val catalogBaseUrl = System.getenv("PUBLIC_CATALOG_URL")?.takeIf { it.isNotBlank() }
        ?: "http://localhost:5173"
    val sfaApiUrl = System.getenv("SFA_API_URL")?.takeIf { it.isNotBlank() }
    val store = SessionStore()
    val rooms = RoomManager()
    val catalogValidator = sfaApiUrl
        ?.let { SfaCatalogLinkValidator(HttpClient(OkHttp), it) }
        ?: DisabledCatalogLinkValidator

    routing {
        get("/health") { call.respondText("ok") }
        get("/version") {
            call.respond(Version(name = "trovatacast-signaling", version = "0.1.0"))
        }
    }
    sessionRoutes(store, catalogBaseUrl, catalogValidator, IceConfig.fromEnv())
    signalingRoutes(rooms, store)

    val staticRoot = File(System.getenv("STATIC_DIR")?.takeIf { it.isNotBlank() } ?: "static")
    if (staticRoot.isDirectory) {
        routing {
            staticFiles("/", staticRoot) {
                default("index.html")
            }
        }
    }
}

@Serializable
private data class Version(val name: String, val version: String)
