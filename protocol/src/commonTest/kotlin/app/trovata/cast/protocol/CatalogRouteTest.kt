package app.trovata.cast.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogRouteTest {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun everyRouteRoundTrips() {
        val routes = listOf(
            CatalogRoute.Inicio,
            CatalogRoute.Menu,
            CatalogRoute.Todos,
            CatalogRoute.Secao(tabela = "categoria", tabelaId = "12"),
            CatalogRoute.Carrinho,
            CatalogRoute.Favoritos,
        )
        routes.forEach { route ->
            val raw = json.encodeToString(CatalogRoute.serializer(), route)
            assertEquals(route, json.decodeFromString(CatalogRoute.serializer(), raw))
        }
    }

    @Test
    fun secaoAcceptsAnyBackendTable() {
        val route = CatalogRoute.Secao(tabela = "grupo_produto", tabelaId = "17")
        val raw = json.encodeToString(CatalogRoute.serializer(), route)
        assertEquals(route, json.decodeFromString(CatalogRoute.serializer(), raw))
        assertTrue(raw.contains("\"type\":\"secao\""))
        assertTrue(raw.contains("\"tabela\":\"grupo_produto\""))
    }

    @Test
    fun pathSegmentsMirrorVueRouter() {
        assertEquals(emptyList(), CatalogRoute.Inicio.pathSegments())
        assertEquals(listOf("menu"), CatalogRoute.Menu.pathSegments())
        assertEquals(listOf("todos"), CatalogRoute.Todos.pathSegments())
        assertEquals(listOf("marca", "3"), CatalogRoute.Secao(tabela = "marca", tabelaId = "3").pathSegments())
        assertEquals(listOf("carrinho"), CatalogRoute.Carrinho.pathSegments())
        assertEquals(listOf("favoritos"), CatalogRoute.Favoritos.pathSegments())
    }

    @Test
    fun sessionTokenNeverTravelsInSyncedQuery() {
        val query = mapOf(
            "search" to "camisa",
            "page" to "3",
            "live" to "kP3xq9Trz",
            "token" to "jwt-legado",
            "utm_source" to "whatsapp",
        )
        val synced = query.retainSyncedQuery()
        assertEquals(mapOf("search" to "camisa", "page" to "3"), synced)
        assertTrue("live" !in synced)
        assertTrue("token" !in synced)
    }

    @Test
    fun viewStateRoundTripsWithFocus() {
        val view = ViewState(
            route = CatalogRoute.Todos,
            query = mapOf("marca" to "9"),
            focus = ProductFocus(produtoPreId = 8813, produtoPre1Id = 4410, complemento1Id = 44),
        )
        val raw = json.encodeToString(ViewState.serializer(), view)
        assertEquals(view, json.decodeFromString(ViewState.serializer(), raw))
    }
}
