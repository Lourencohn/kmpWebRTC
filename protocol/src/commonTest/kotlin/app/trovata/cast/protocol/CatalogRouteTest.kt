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
    fun sectionAcceptsAnyBackendTable() {
        val route = CatalogRoute.Secao(tabela = "grupo_produto", tabelaId = "17")
        val raw = json.encodeToString(CatalogRoute.serializer(), route)
        assertEquals(route, json.decodeFromString(CatalogRoute.serializer(), raw))
        assertTrue(raw.contains("\"type\":\"secao\""))
        assertTrue(raw.contains("\"tabela\":\"grupo_produto\""))
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
