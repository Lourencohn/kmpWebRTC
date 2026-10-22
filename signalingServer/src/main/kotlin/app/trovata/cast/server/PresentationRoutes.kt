package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.PresentationAudioContentType
import app.trovata.cast.protocol.PresentationCreateRequest
import app.trovata.cast.protocol.PresentationCreateResponse
import app.trovata.cast.protocol.PresentationInfo
import app.trovata.cast.protocol.PresentationTimelineContentType
import app.trovata.cast.protocol.PresentationUpload
import app.trovata.cast.protocol.buildPresentationUrl
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.security.SecureRandom

private const val PRESENTATION_ID_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private const val PRESENTATION_ID_LENGTH = 16
private const val UPLOAD_TTL_SECONDS = 15L * 60
private const val PLAYBACK_TTL_SECONDS = 6L * 60 * 60
private const val MAX_DURATION_MS = 60L * 60 * 1000

data class PresentationStorageConfig(
    val endpoint: String,
    val bucket: String,
    val region: String,
    val accessKey: String,
    val secretKey: String,
) {
    fun presigner(): S3Presigner = S3Presigner(endpoint, bucket, region, accessKey, secretKey)

    companion object {
        fun fromEnv(env: (String) -> String? = System::getenv): PresentationStorageConfig? {
            fun value(name: String): String? = env(name)?.trim()?.takeIf { it.isNotEmpty() }
            return PresentationStorageConfig(
                endpoint = value("PRESENTATION_S3_ENDPOINT") ?: return null,
                bucket = value("PRESENTATION_S3_BUCKET") ?: return null,
                region = value("PRESENTATION_S3_REGION") ?: "auto",
                accessKey = value("PRESENTATION_S3_ACCESS_KEY") ?: return null,
                secretKey = value("PRESENTATION_S3_SECRET_KEY") ?: return null,
            )
        }
    }
}

object PresentationKeys {
    fun timeline(presentationId: String): String = "presentations/$presentationId/timeline.json"

    fun audio(presentationId: String): String = "presentations/$presentationId/audio.m4a"
}

class PresentationIds(private val random: SecureRandom = SecureRandom()) {
    fun next(): String = buildString(PRESENTATION_ID_LENGTH) {
        repeat(PRESENTATION_ID_LENGTH) {
            append(PRESENTATION_ID_ALPHABET[random.nextInt(PRESENTATION_ID_ALPHABET.length)])
        }
    }

    companion object {
        fun isWellFormed(candidate: String): Boolean =
            candidate.length == PRESENTATION_ID_LENGTH && candidate.all { it in PRESENTATION_ID_ALPHABET }
    }
}

fun Application.presentationRoutes(
    presigner: S3Presigner?,
    catalogBaseUrl: String,
    validator: CatalogLinkValidator = DisabledCatalogLinkValidator,
    ids: PresentationIds = PresentationIds(),
    now: () -> Long = System::currentTimeMillis,
) {
    routing {
        route("/presentations") {
            post {
                val storage = presigner ?: return@post call.respondStorageDisabled()
                call.createPresentation(storage, catalogBaseUrl, validator, ids, now)
            }
            get("/{id}") {
                val storage = presigner ?: return@get call.respondStorageDisabled()
                call.fetchPresentation(storage, now)
            }
        }
    }
}

private suspend fun ApplicationCall.respondStorageDisabled() {
    respond(
        HttpStatusCode.ServiceUnavailable,
        ErrorResponse("presentations_disabled", "Gravação de apresentações não está configurada no servidor"),
    )
}

private suspend fun ApplicationCall.createPresentation(
    presigner: S3Presigner,
    catalogBaseUrl: String,
    validator: CatalogLinkValidator,
    ids: PresentationIds,
    now: () -> Long,
) {
    val bearer = bearerToken()
    if (bearer == null) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("missing_auth", "Entre com o vendedor para gravar"))
        return
    }
    val body = try {
        receive<PresentationCreateRequest>()
    } catch (_: Throwable) {
        respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_body", "JSON inválido"))
        return
    }
    body.validationError()?.let { error ->
        respond(HttpStatusCode.BadRequest, error)
        return
    }

    val check = validator.check(
        empresaSlug = body.empresaSlug,
        catalogoUuid = body.catalogoUuid,
        bearerToken = bearer,
    )
    if (check is CatalogLinkCheck.Rejected) {
        respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(check.code, check.message))
        return
    }

    val presentationId = ids.next()
    respond(
        HttpStatusCode.Created,
        PresentationCreateResponse(
            presentationId = presentationId,
            shareUrl = buildPresentationUrl(catalogBaseUrl, body.empresaSlug, body.catalogoUuid, presentationId),
            timeline = PresentationUpload(
                url = presigner.presignPut(PresentationKeys.timeline(presentationId), UPLOAD_TTL_SECONDS),
                contentType = PresentationTimelineContentType,
            ),
            audio = PresentationUpload(
                url = presigner.presignPut(PresentationKeys.audio(presentationId), UPLOAD_TTL_SECONDS),
                contentType = PresentationAudioContentType,
            ),
            uploadExpiresAtMs = now() + UPLOAD_TTL_SECONDS * 1000,
        ),
    )
}

private fun PresentationCreateRequest.validationError(): ErrorResponse? = when {
    empresaSlug.isBlank() -> ErrorResponse("missing_empresa", "Informe a empresa do catálogo")
    catalogoUuid.isBlank() -> ErrorResponse("missing_catalogo", "Informe o catálogo link da apresentação")
    sellerId.isBlank() -> ErrorResponse("missing_seller", "Informe o vendedor da apresentação")
    durationMs <= 0 -> ErrorResponse("empty_recording", "A gravação está vazia")
    durationMs > MAX_DURATION_MS -> ErrorResponse("recording_too_long", "A gravação passa de 60 minutos")
    audioContentType != PresentationAudioContentType ->
        ErrorResponse("unsupported_audio", "Formato de áudio não suportado: $audioContentType")
    else -> null
}

private suspend fun ApplicationCall.fetchPresentation(presigner: S3Presigner, now: () -> Long) {
    val presentationId = parameters["id"].orEmpty()
    if (!PresentationIds.isWellFormed(presentationId)) {
        respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Apresentação não encontrada"))
        return
    }
    respond(
        PresentationInfo(
            presentationId = presentationId,
            timelineUrl = presigner.presignGet(PresentationKeys.timeline(presentationId), PLAYBACK_TTL_SECONDS),
            audioUrl = presigner.presignGet(PresentationKeys.audio(presentationId), PLAYBACK_TTL_SECONDS),
            expiresAtMs = now() + PLAYBACK_TTL_SECONDS * 1000,
        ),
    )
}
