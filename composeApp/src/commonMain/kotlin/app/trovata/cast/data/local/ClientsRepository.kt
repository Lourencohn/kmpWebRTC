package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.trovata.cast.db.ClientEntity
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class CatalogClient(
    val id: Long,
    val ref: String,
    val name: String,
    val legalName: String?,
    val document: String?,
    val phone: String?,
    val email: String?,
    val contact: String?,
    val active: Boolean,
)

class ClientsRepository(private val db: TrovataDatabase) {
    private val people = db.peopleQueries

    fun observeCount(): Flow<Long> =
        people.countClients().asFlow().mapToOne(Dispatchers.Default)

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
}

private fun ClientEntity.toDomain(): CatalogClient = CatalogClient(
    id = id,
    ref = idErp ?: id.toString(),
    name = nomeFantasia ?: razaoSocial ?: (idErp ?: id.toString()),
    legalName = razaoSocial,
    document = cpfCnpj,
    phone = celular ?: telefone,
    email = email,
    contact = contato,
    active = situacao == "A",
)
