package app.trovata.cast.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class CallInterestRepositoryTest {

    private val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migrationFromThePreviousVersionAddsPhoneAndInterest() = runBlocking {
        TrovataDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE CallInterestEntity", 0)
        driver.execute(null, "ALTER TABLE SessionEntity DROP COLUMN clientPhone", 0)

        TrovataDatabase.Schema.migrate(driver, PREVIOUS_VERSION, TrovataDatabase.Schema.version)
        val database = TrovataDatabase(driver)
        val sessions = SessionsRepository(database)
        val interests = CallInterestRepository(database)
        sessions.persistCreated(request(), response(), createdAtMs = 1_000, client = SessionClientNotes(phone = "(11) 98888-7777"))
        interests.apply("ses_live", CallInterestEvent.Focused(77, InterestSource.Client, atMs = 2_000))

        assertEquals(PREVIOUS_VERSION + 1, TrovataDatabase.Schema.version)
        assertEquals("(11) 98888-7777", sessions.findById("ses_live")?.clientPhone)
        assertEquals(1, interests.listForSession("ses_live").single().clientOpens)
    }

    @Test
    fun signalsOfTheCallAddUpPerProduct() = runBlocking {
        val repository = freshRepository()
        repository.apply("ses_live", CallInterestEvent.Focused(77, InterestSource.Client, atMs = 1_000))
        repository.apply("ses_live", CallInterestEvent.Focused(77, InterestSource.Client, atMs = 2_000))
        repository.apply("ses_live", CallInterestEvent.Focused(77, InterestSource.Seller, atMs = 3_000))
        repository.apply("ses_live", CallInterestEvent.Drafted(77, units = 12, atMs = 4_000))
        repository.apply("ses_live", CallInterestEvent.Drafted(77, units = 6, atMs = 5_000))
        repository.apply("ses_live", CallInterestEvent.Named(77, ref = "AN-104", name = "Vestido Midi", imageUrl = null))
        repository.apply("ses_other", CallInterestEvent.Focused(77, InterestSource.Client, atMs = 1_000))

        assertEquals(
            CallInterest(
                produtoPreId = 77,
                clientOpens = 2,
                sellerShows = 1,
                draftUnits = 12,
                cartUnits = 0,
                ref = "AN-104",
                name = "Vestido Midi",
                imageUrl = null,
                firstAtMs = 1_000,
                lastAtMs = 5_000,
            ),
            repository.listForSession("ses_live").single(),
        )
    }

    @Test
    fun cartSnapshotReplacesThePreviousOne() = runBlocking {
        val repository = freshRepository()
        repository.apply("ses_live", CallInterestEvent.Focused(77, InterestSource.Client, atMs = 1_000))
        repository.apply("ses_live", CallInterestEvent.Focused(88, InterestSource.Client, atMs = 1_500))

        repository.saveCart(
            "ses_live",
            listOf(
                CartProduct(77, "AN-104", "Vestido Midi", "https://img/77.jpg", 4),
                CartProduct(77, "AN-104", "Vestido Midi", "https://img/77.jpg", 4),
                CartProduct(99, "BT-9", "Blusa Tricô", null, 6),
            ),
            atMs = 2_000,
        )
        val first = repository.listForSession("ses_live").associateBy { it.produtoPreId }
        assertEquals(8, first.getValue(77).cartUnits)
        assertEquals("https://img/77.jpg", first.getValue(77).imageUrl)
        assertEquals(6, first.getValue(99).cartUnits)
        assertEquals(listOf(88L), first.values.toList().leftOut().map { it.produtoPreId })

        repository.saveCart("ses_live", listOf(CartProduct(99, null, null, null, 6)), atMs = 3_000)
        val second = repository.listForSession("ses_live").associateBy { it.produtoPreId }
        assertEquals(0, second.getValue(77).cartUnits)
        assertEquals("Vestido Midi", second.getValue(77).name)
        assertEquals("Blusa Tricô", second.getValue(99).name)
        assertEquals(setOf(77L, 88L), second.values.toList().leftOut().map { it.produtoPreId }.toSet())
    }

    @Test
    fun recorderKeepsWritingAfterTheCallScreenIsGone() = runBlocking {
        val repository = freshRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val recorder = CallInterestRecorder(repository, scope)

        recorder.record("ses_live", CallInterestEvent.Focused(77, InterestSource.Client, atMs = 1_000))
        recorder.recordCart("ses_live", listOf(CartProduct(77, "AN-104", "Vestido Midi", null, 8)), atMs = 2_000)

        val stored = withTimeout(5_000) {
            repository.observeForSession("ses_live").first { rows -> rows.any { it.cartUnits > 0 } }
        }
        assertEquals(1, stored.single().clientOpens)
        assertEquals(8, stored.single().cartUnits)
        scope.cancel()
    }

    @Test
    fun clientIsFoundByEmailIgnoringCaseAndSpaces() = runBlocking {
        TrovataDatabase.Schema.create(driver)
        val database = TrovataDatabase(driver)
        database.peopleQueries.upsertClient(
            id = 101, idErp = "C-101", vendedorIdErp = null, cidadeIdErp = null, situacao = "A",
            nomeFantasia = "Loja Bella", razaoSocial = null, cpfCnpj = null, endereco = null, bairro = null,
            cep = null, telefone = "1133334444", celular = "11988887777", email = " Compras@Bella.com.br",
            contato = null, tabelaPrecoId = null, prazoId = null, percDesconto = null, updatedAt = null,
            updatedAtMs = 1, deletedAt = null,
        )
        val clients = ClientsRepository(database)

        assertEquals("11988887777", clients.findByEmail("compras@bella.com.br ")?.phone)
        assertNull(clients.findByEmail("outra@loja.com"))
        assertNull(clients.findByEmail(null))
    }

    private fun freshRepository(): CallInterestRepository {
        TrovataDatabase.Schema.create(driver)
        return CallInterestRepository(TrovataDatabase(driver))
    }

    private fun request() = SessionCreateRequest(
        empresaSlug = "buba",
        catalogoUuid = "uuid-1",
        sellerId = "42",
        sellerName = "Lourenço",
        clientName = "Marina",
        clientEmail = "marina@bella.com.br",
    )

    private fun response() = SessionCreateResponse(
        sessionId = "ses_live",
        token = "kP3xq9",
        url = "https://trovata.app.br/catalogo-link-view/buba/uuid-1?live=kP3xq9",
        expiresAtMs = 10_000,
    )

    private companion object {
        const val PREVIOUS_VERSION = 5L
    }
}
