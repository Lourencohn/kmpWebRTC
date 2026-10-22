package app.trovata.cast.data.sync

import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.SfaApi
import app.trovata.cast.data.remote.sfa.SfaParse
import app.trovata.cast.data.remote.sfa.dto.ClientDto
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

private const val CLIENTS_RESOURCE = "clientes"
private const val CLIENTS_PER_PAGE = 1000
private const val DELETE_BATCH_SIZE = 900

class ClientSyncService(
    private val sfaApi: SfaApi,
    private val db: TrovataDatabase,
    private val json: Json = HttpClientFactory.sfaJson,
) {
    private val people = db.peopleQueries
    private val sync = db.syncStateQueries

    suspend fun syncClients(): Unit = withContext(Dispatchers.Default) {
        val state = sync.selectSyncState(CLIENTS_RESOURCE).executeAsOneOrNull()
        val cursor = state?.cursorUpdatedAt
        sync.upsertSyncState(CLIENTS_RESOURCE, cursor, state?.lastSyncAtMs, 0, 1, null)
        var page = 1
        var maxUpdated: String? = cursor
        while (true) {
            when (val result = sfaApi.fetchPageRaw(CLIENTS_RESOURCE, page, CLIENTS_PER_PAGE, cursor)) {
                is ApiResult.Fail -> {
                    sync.upsertSyncState(CLIENTS_RESOURCE, cursor, state?.lastSyncAtMs, page.toLong(), 0, "${result.code}: ${result.message}")
                    return@withContext
                }
                is ApiResult.Ok -> {
                    val envelope = result.value
                    val clients = envelope.data.orEmpty().mapNotNull { element ->
                        runCatching { json.decodeFromJsonElement<ClientDto>(element) }.getOrNull()
                    }
                    val deletedIds = envelope.deletedIds.orEmpty()
                    db.transaction {
                        clients.forEach(::upsert)
                        deletedIds.chunked(DELETE_BATCH_SIZE).forEach { people.deleteClientsByIds(it) }
                    }
                    clients.mapNotNull { it.updatedAt }.maxOrNull()?.let { latest ->
                        if (maxUpdated == null || latest > maxUpdated!!) maxUpdated = latest
                    }
                    val pagination = envelope.pagination
                    if (pagination == null || pagination.currentPage >= pagination.lastPage) break
                    page++
                }
            }
        }
        sync.upsertSyncState(CLIENTS_RESOURCE, maxUpdated, Clock.System.now().toEpochMilliseconds(), 0, 0, null)
    }

    private fun upsert(client: ClientDto) = people.upsertClient(
        client.id, client.idErp, client.vendedorIdErp, client.cidadeIdErp, client.situacao, client.nomeFantasia,
        client.razaoSocial, client.cpfCnpj, client.endereco, client.bairro, client.cep, client.telefone, client.celular,
        client.email, client.contato, client.tabelaPrecoId, client.prazoId, client.percDesconto?.toDoubleOrNull(),
        client.updatedAt, SfaParse.parseIsoToMs(client.updatedAt), client.deletedAt,
    )
}
