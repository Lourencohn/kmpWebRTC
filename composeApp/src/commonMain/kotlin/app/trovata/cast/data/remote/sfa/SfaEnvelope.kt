package app.trovata.cast.data.remote.sfa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SfaListEnvelope<T>(
    val data: List<T>? = null,
    @SerialName("deleted_ids") val deletedIds: List<Long>? = null,
    val pagination: SfaPagination? = null,
)

@Serializable
data class SfaPagination(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    val total: Int = 0,
)

@Serializable
data class SfaObjectEnvelope<T>(
    val data: T? = null,
)
