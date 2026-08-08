package app.trovata.cast.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogLinkValidatorTest {

    private val slug = "buba-teste"
    private val uuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f"

    private fun validator(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<SfaCatalogLinkValidator, MutableList<String>> {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls.add(request.url.toString())
            handler(request)
        }
        return SfaCatalogLinkValidator(HttpClient(engine), "https://api-int.trovata.app.br/") to urls
    }

    @Test
    fun catalogoValidoPassaEBateNaRotaPublicaDaVitrine() = runTest {
        val (subject, urls) = validator {
            respond(
                content = """{"data":[],"email_catalogo_link":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertIs<CatalogLinkCheck.Valid>(subject.check(slug, uuid, bearerToken = null))
        assertEquals(
            "https://api-int.trovata.app.br/catalogos-links/$slug/$uuid/clientes-liberados",
            urls.single(),
        )
    }

    @Test
    fun catalogoInexistenteEhRejeitado() = runTest {
        val (subject, _) = validator { respondError(HttpStatusCode.NotFound) }

        val result = subject.check(slug, uuid, bearerToken = null)
        val rejected = assertIs<CatalogLinkCheck.Rejected>(result)
        assertEquals("catalogo_not_found", rejected.code)
    }

    @Test
    fun catalogoExpiradoExplicaComoResolver() = runTest {
        val (subject, _) = validator {
            respond(
                content = """{"message":"Catalogo expirado"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val rejected = assertIs<CatalogLinkCheck.Rejected>(subject.check(slug, uuid, null))
        assertEquals("catalogo_indisponivel", rejected.code)
        assertTrue(rejected.message.contains("expirado", ignoreCase = true))
        assertTrue(rejected.message.contains("Renove"))
    }

    @Test
    fun catalogoInativoEhRejeitado() = runTest {
        val (subject, _) = validator {
            respond(
                content = """{"message":"Catalogo não está ativo"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val rejected = assertIs<CatalogLinkCheck.Rejected>(subject.check(slug, uuid, null))
        assertEquals("Catálogo inativo.", rejected.message)
    }

    @Test
    fun sfaForaDoArNaoBloqueiaACriacaoDaSessao() = runTest {
        val (indisponivel, _) = validator { respondError(HttpStatusCode.BadGateway) }
        assertIs<CatalogLinkCheck.Skipped>(indisponivel.check(slug, uuid, null))

        val (semRede, _) = validator { throw RuntimeException("connection reset") }
        assertIs<CatalogLinkCheck.Skipped>(semRede.check(slug, uuid, null))
    }

    @Test
    fun erro500MascaradoDoLaravelNaoBloqueia() = runTest {
        val (subject, _) = validator {
            respond(
                content = """{"message":"Server Error"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        assertIs<CatalogLinkCheck.Skipped>(subject.check(slug, uuid, null))
    }

    @Test
    fun repassaOBearerDoVendedorQuandoExiste() = runTest {
        var authorization: String? = null
        val engine = MockEngine { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val subject = SfaCatalogLinkValidator(HttpClient(engine), "https://api-int.trovata.app.br")

        subject.check(slug, uuid, bearerToken = "jwt-do-vendedor")
        assertEquals("Bearer jwt-do-vendedor", authorization)
    }

    @Test
    fun validadorDesligadoNaoConsultaNada() = runTest {
        assertIs<CatalogLinkCheck.Skipped>(DisabledCatalogLinkValidator.check(slug, uuid, null))
    }
}
