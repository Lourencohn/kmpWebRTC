package app.trovata.cast.feature.presentation

import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.local.StoredPresentation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dayMs = 24L * 60 * 60 * 1000
    private val now = at(2026, 10, 18, 15, 0)

    private val buba = Company(id = 1, slug = "buba-teste", name = "Buba", legalName = null, cnpj = null, logoUrl = null)
    private val atelier = Company(id = 2, slug = "atelier-norte", name = "Atelier Norte", legalName = null, cnpj = null, logoUrl = null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(TimeZone.UTC).toEpochMilliseconds()

    private fun presentation(
        id: String,
        recordedAtMs: Long,
        uploadedAtMs: Long = recordedAtMs,
        empresaSlug: String = "buba-teste",
        durationMs: Long = 63_000,
        eventCount: Int = 37,
    ) = StoredPresentation(
        presentationId = id,
        shareUrl = "https://staging.trovata.app.br/catalogo-link-view/$empresaSlug/uuid?apresentacao=$id",
        empresaSlug = empresaSlug,
        catalogoUuid = "uuid",
        catalogoNome = "Outono 26",
        sellerId = "31",
        sellerName = "Marina Prado",
        recordedAtMs = recordedAtMs,
        durationMs = durationMs,
        eventCount = eventCount,
        uploadedAtMs = uploadedAtMs,
    )

    private fun model(
        companies: Flow<Company?> = flowOf(buba),
        byCompany: Map<String, Flow<List<StoredPresentation>>> = emptyMap(),
        clock: Flow<Long> = flowOf(now),
        removed: MutableList<String> = mutableListOf(),
    ) = RecordingsScreenModel(
        companies = companies,
        observe = { slug -> byCompany[slug] ?: flowOf(emptyList()) },
        removePresentation = { removed += it },
        clock = clock,
        timeZone = TimeZone.UTC,
    )

    private fun expiringAt(id: String, expiresAtMs: Long) =
        presentation(id, recordedAtMs = expiresAtMs - 30 * dayMs)

    @Test
    fun separatesLinksStillPlayingFromExpiredOnesNewestFirst() = runTest(dispatcher) {
        val older = presentation("older", recordedAtMs = now - 2 * dayMs)
        val newest = presentation("newest", recordedAtMs = now - 60_000)
        val expired = presentation("expired", recordedAtMs = now - 31 * dayMs)
        val model = model(byCompany = mapOf("buba-teste" to flowOf(listOf(older, expired, newest))))
        advanceUntilIdle()

        val state = model.state.value
        assertEquals(listOf("newest", "older"), state.available.map { it.presentationId })
        assertEquals(listOf("expired"), state.expired.map { it.presentationId })
        assertEquals("2 disponíveis · 1 expirada", state.subtitle)
        assertEquals("Buba", state.companyName)
        model.onDispose()
    }

    @Test
    fun labelsSayWhenItWasRecordedAndUntilWhenTheLinkPlays() = runTest(dispatcher) {
        val today = presentation("today", recordedAtMs = at(2026, 10, 18, 14, 0), eventCount = 1)
        val yesterday = presentation("yesterday", recordedAtMs = at(2026, 10, 17, 9, 5))
        val earlier = presentation("earlier", recordedAtMs = at(2026, 10, 1, 10, 30), durationMs = 125_400)
        val model = model(byCompany = mapOf("buba-teste" to flowOf(listOf(today, yesterday, earlier))))
        advanceUntilIdle()

        val items = model.state.value.available.associateBy { it.presentationId }
        assertEquals("Gravada hoje às 14:00", items.getValue("today").recordedLabel)
        assertEquals("Gravada ontem às 09:05", items.getValue("yesterday").recordedLabel)
        assertEquals("Gravada em 01/10 às 10:30", items.getValue("earlier").recordedLabel)
        assertEquals("Disponível até 17/11", items.getValue("today").availabilityLabel)
        assertEquals("Disponível até 31/10", items.getValue("earlier").availabilityLabel)
        assertEquals("01:03", items.getValue("today").durationLabel)
        assertEquals("02:05", items.getValue("earlier").durationLabel)
        assertEquals("1 ação", items.getValue("today").actionsLabel)
        assertEquals("37 ações", items.getValue("yesterday").actionsLabel)
        assertTrue(items.getValue("today").shareMessage.endsWith(today.shareUrl))
        model.onDispose()
    }

    @Test
    fun recordingFromAnotherYearShowsTheYear() = runTest(dispatcher) {
        val clock = flowOf(at(2027, 1, 5, 10, 0))
        val old = presentation("old", recordedAtMs = at(2026, 12, 20, 8, 15))
        val model = model(byCompany = mapOf("buba-teste" to flowOf(listOf(old))), clock = clock)
        advanceUntilIdle()

        assertEquals("Gravada em 20/12/2026 às 08:15", model.state.value.available.single().recordedLabel)
        model.onDispose()
    }

    @Test
    fun theLastDaysWarnThatTheLinkIsAboutToStop() = runTest(dispatcher) {
        val list = listOf(
            expiringAt("hoje", now + 2 * 60 * 60 * 1000),
            expiringAt("amanha", now + dayMs),
            expiringAt("tres", now + 3 * dayMs),
            expiringAt("quatro", now + 4 * dayMs),
            expiringAt("acabou", now - 1),
        )
        val model = model(byCompany = mapOf("buba-teste" to flowOf(list)))
        advanceUntilIdle()

        val items = (model.state.value.available + model.state.value.expired).associateBy { it.presentationId }
        assertEquals("Expira hoje", items.getValue("hoje").availabilityLabel)
        assertEquals("Expira amanhã", items.getValue("amanha").availabilityLabel)
        assertEquals("Expira em 3 dias", items.getValue("tres").availabilityLabel)
        assertEquals("Disponível até 22/10", items.getValue("quatro").availabilityLabel)
        assertEquals("Expirou em 18/10", items.getValue("acabou").availabilityLabel)
        assertEquals(RecordingAvailability.ExpiringSoon, items.getValue("tres").availability)
        assertEquals(RecordingAvailability.Available, items.getValue("quatro").availability)
        assertEquals(RecordingAvailability.Expired, items.getValue("acabou").availability)
        model.onDispose()
    }

    @Test
    fun theClockMovesALinkToExpiredWithoutReopeningTheTab() = runTest(dispatcher) {
        val clock = MutableStateFlow(now)
        val model = model(
            byCompany = mapOf("buba-teste" to flowOf(listOf(expiringAt("quase", now + 60_000)))),
            clock = clock,
        )
        advanceUntilIdle()
        assertEquals(listOf("quase"), model.state.value.available.map { it.presentationId })

        clock.value = now + 120_000
        advanceUntilIdle()
        assertTrue(model.state.value.available.isEmpty())
        assertEquals(listOf("quase"), model.state.value.expired.map { it.presentationId })
        model.onDispose()
    }

    @Test
    fun onlyTheActiveCompanyRecordingsAreListed() = runTest(dispatcher) {
        val companies = MutableStateFlow<Company?>(buba)
        val model = model(
            companies = companies,
            byCompany = mapOf(
                "buba-teste" to flowOf(listOf(presentation("da-buba", recordedAtMs = now))),
                "atelier-norte" to flowOf(listOf(presentation("do-atelier", recordedAtMs = now, empresaSlug = "atelier-norte"))),
            ),
        )
        advanceUntilIdle()
        assertEquals(listOf("da-buba"), model.state.value.available.map { it.presentationId })

        companies.value = atelier
        advanceUntilIdle()
        assertEquals(listOf("do-atelier"), model.state.value.available.map { it.presentationId })
        assertEquals("Atelier Norte", model.state.value.companyName)
        model.onDispose()
    }

    @Test
    fun withoutCompanyTheTabShowsTheEmptyGuide() = runTest(dispatcher) {
        val model = model(companies = flowOf(null))
        assertTrue(model.state.value.isLoading)
        assertFalse(model.state.value.isEmpty)
        advanceUntilIdle()

        assertFalse(model.state.value.isLoading)
        assertTrue(model.state.value.isEmpty)
        assertEquals("Grave uma vez e mande para vários clientes", model.state.value.subtitle)
        model.onDispose()
    }

    @Test
    fun removingTakesTheRecordingOutOfTheLocalList() = runTest(dispatcher) {
        val removed = mutableListOf<String>()
        val model = model(removed = removed)
        model.removeFromList("expired")
        advanceUntilIdle()

        assertEquals(listOf("expired"), removed)
        model.onDispose()
    }
}
