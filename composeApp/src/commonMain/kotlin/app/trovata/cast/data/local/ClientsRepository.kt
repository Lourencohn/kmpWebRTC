package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.trovata.cast.db.ClientEntity
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class CatalogClient(
    val id: Long,
    val ref: String,
    val name: String,
    val legalName: String?,
    val document: String?,
    val city: String?,
    val phone: String?,
    val email: String?,
    val contact: String?,
    val priceTableId: Long?,
    val sellerErp: String?,
    val active: Boolean,
)

class ClientsRepository(private val db: TrovataDatabase) {
    private val people = db.peopleQueries

    fun observeClients(): Flow<List<CatalogClient>> =
        people.selectAllClients().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toDomain() } }

    fun observeCount(): Flow<Long> =
        people.countClients().asFlow().mapToOne(Dispatchers.Default)

    suspend fun byId(id: Long): CatalogClient? = withContext(Dispatchers.Default) {
        people.selectClientById(id).executeAsOneOrNull()?.toDomain()
    }

    suspend fun forSeller(sellerErp: String): List<CatalogClient> = withContext(Dispatchers.Default) {
        people.selectClientsByVendedor(sellerErp).executeAsList().map { it.toDomain() }
    }

    suspend fun search(query: String, limit: Int = 60): List<CatalogClient> = withContext(Dispatchers.Default) {
        val q = query.trim()
        val rows = if (q.isEmpty()) {
            people.firstClients(limit.toLong()).executeAsList()
        } else {
            val like = "%$q%"
            people.searchClients(like, like, like, like, limit.toLong()).executeAsList()
        }
        rows.map { it.toDomain() }
    }

    suspend fun count(): Long = withContext(Dispatchers.Default) {
        people.countClients().executeAsOne()
    }

    suspend fun isEmpty(): Boolean = withContext(Dispatchers.Default) {
        people.countClients().executeAsOne() == 0L
    }
}

private fun ClientEntity.toDomain(): CatalogClient = CatalogClient(
    id = id,
    ref = idErp ?: id.toString(),
    name = nomeFantasia ?: razaoSocial ?: (idErp ?: id.toString()),
    legalName = razaoSocial,
    document = cpfCnpj,
    city = cidadeIdErp,
    phone = celular ?: telefone,
    email = email,
    contact = contato,
    priceTableId = tabelaPrecoId,
    sellerErp = vendedorIdErp,
    active = situacao == "A",
)
