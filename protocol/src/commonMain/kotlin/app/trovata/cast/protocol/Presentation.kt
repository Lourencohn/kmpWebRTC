package app.trovata.cast.protocol

import kotlinx.serialization.Serializable

const val PresentationQueryParam = "apresentacao"
const val LiveEmbedRecorderValue = "recorder"
const val PresentationTimelineVersion = 1
const val PresentationTimelineContentType = "application/json"
const val PresentationAudioContentType = "audio/mp4"

@Serializable
data class PresentationCreateRequest(
    val empresaSlug: String,
    val catalogoUuid: String,
    val sellerId: String,
    val sellerName: String,
    val durationMs: Long,
    val catalogoNome: String? = null,
    val audioContentType: String = PresentationAudioContentType,
)

@Serializable
data class PresentationUpload(
    val url: String,
    val contentType: String,
)

@Serializable
data class PresentationCreateResponse(
    val presentationId: String,
    val shareUrl: String,
    val timeline: PresentationUpload,
    val audio: PresentationUpload,
    val uploadExpiresAtMs: Long,
)

@Serializable
data class PresentationInfo(
    val presentationId: String,
    val timelineUrl: String,
    val audioUrl: String,
    val expiresAtMs: Long,
)

@Serializable
data class PresentationEvent(
    val t: Long,
    val message: DataChannelMessage,
)

@Serializable
data class PresentationTimeline(
    val presentationId: String,
    val empresaSlug: String,
    val catalogoUuid: String,
    val sellerName: String,
    val recordedAtMs: Long,
    val durationMs: Long,
    val events: List<PresentationEvent>,
    val catalogoNome: String? = null,
    val audioContentType: String = PresentationAudioContentType,
    val version: Int = PresentationTimelineVersion,
)

fun PresentationTimeline.encode(): String =
    DataChannelJson.encodeToString(PresentationTimeline.serializer(), this)

fun buildPresentationUrl(
    catalogBaseUrl: String,
    empresaSlug: String,
    catalogoUuid: String,
    presentationId: String,
): String =
    "${catalogPagePath(catalogBaseUrl, empresaSlug, catalogoUuid)}?$PresentationQueryParam=$presentationId"

fun buildRecorderEmbedUrl(
    catalogBaseUrl: String,
    empresaSlug: String,
    catalogoUuid: String,
): String =
    "${catalogPagePath(catalogBaseUrl, empresaSlug, catalogoUuid)}?$LiveEmbedQueryParam=$LiveEmbedRecorderValue"

private fun catalogPagePath(catalogBaseUrl: String, empresaSlug: String, catalogoUuid: String): String =
    "${catalogBaseUrl.trimEnd('/')}/catalogo-link-view/$empresaSlug/$catalogoUuid"
