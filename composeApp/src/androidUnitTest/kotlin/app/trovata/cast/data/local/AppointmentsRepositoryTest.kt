package app.trovata.cast.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.trovata.cast.db.TrovataDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class AppointmentsRepositoryTest {

    private val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val owner = AppointmentOwner(empresaSlug = "buba", sellerId = "42")

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migrationFromThePreviousVersionCreatesTheAgenda() = runBlocking {
        TrovataDatabase.Schema.create(driver)
        driver.execute(null, "DROP VIEW AppointmentWithOrder", 0)
        driver.execute(null, "DROP TABLE AppointmentEntity", 0)

        TrovataDatabase.Schema.migrate(driver, PREVIOUS_VERSION, TrovataDatabase.Schema.version)
        val repository = AppointmentsRepository(TrovataDatabase(driver))
        repository.save(appointment("apt-1"))

        assertEquals(PREVIOUS_VERSION + 1, TrovataDatabase.Schema.version)
        assertEquals(listOf("apt-1"), repository.listForOwner(owner).map { it.appointmentId })
    }

    @Test
    fun roundTripKeepsEveryField() = runBlocking {
        val repository = freshRepository()
        val original = appointment("apt-1").copy(
            priority = AppointmentPriority.High,
            status = AppointmentStatus.Done,
            outcome = AppointmentOutcome.FollowUp,
            outcomeNotes = "pediu prazo",
            nextStep = "mandar tabela",
            prepDone = setOf("PickCatalog", "ReviewCart"),
            carrinhoId = 9021,
            followUpOfId = "apt-0",
            rescheduleCount = 2,
        )

        repository.save(original)

        assertEquals(original, repository.listForOwner(owner).single())
    }

    @Test
    fun eachSellerSeesOnlyTheirOwnAgenda() = runBlocking {
        val repository = freshRepository()
        repository.save(appointment("mine"))
        repository.save(appointment("colleague").copy(sellerId = "77"))
        repository.save(appointment("other-company").copy(empresaSlug = "mart"))

        assertEquals(listOf("mine"), repository.listForOwner(owner).map { it.appointmentId })
    }

    @Test
    fun orderClosedInTheLinkedSessionReachesTheAgendaFlow() = runBlocking {
        val database = TrovataDatabase(driver.also { TrovataDatabase.Schema.create(it) })
        val repository = AppointmentsRepository(database)
        val orders = OrderRepository(database)
        repository.save(appointment("apt-1"))
        repository.linkSession("apt-1", "ses_live", atMs = 2_000)

        assertNull(repository.observeForOwner(owner).first().single().linkedOrder)

        orders.persist(
            orderId = "CAR-9021",
            sessionId = "ses_live",
            sessionToken = "kP3xq9",
            clientName = "Loja Bella",
            clientShop = null,
            sellerName = "Lourenço",
            totalCents = 420_000,
            confirmedByMe = true,
            createdAtMs = 3_000,
            lines = emptyList(),
        )

        val linked = withTimeout(5_000) {
            repository.observeForOwner(owner).first { it.single().linkedOrder != null }.single()
        }
        assertEquals(LinkedOrder("CAR-9021", 420_000), linked.linkedOrder)
        assertEquals("ses_live", linked.sessionId)
    }

    private fun freshRepository(): AppointmentsRepository {
        TrovataDatabase.Schema.create(driver)
        return AppointmentsRepository(TrovataDatabase(driver))
    }

    private fun appointment(id: String) = Appointment(
        appointmentId = id,
        empresaSlug = "buba",
        sellerId = "42",
        clienteId = 101,
        clientName = "Loja Bella",
        clientShop = "Bella Comércio de Roupas Ltda",
        clientDocument = "12.345.678/0001-90",
        clientEmail = "compras@bella.com.br",
        clientPhone = "(11) 98888-7777",
        startsAtMs = 1_000_000,
        durationMinutes = 30,
        objective = "Reposição",
        notes = "Levar tabela de inverno",
        priority = AppointmentPriority.Normal,
        status = AppointmentStatus.Scheduled,
        outcome = null,
        outcomeNotes = null,
        nextStep = null,
        prepDone = emptySet(),
        carrinhoId = null,
        sessionId = null,
        followUpOfId = null,
        rescheduleCount = 0,
        createdAtMs = 1_000,
        updatedAtMs = 1_000,
    )

    private companion object {
        const val PREVIOUS_VERSION = 4L
    }
}
