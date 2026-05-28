package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.auth.AuthUser
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.remote.sfa.dto.CompanyDto
import app.trovata.cast.data.remote.sfa.dto.EmpresaUsuarioDto
import app.trovata.cast.data.remote.sfa.dto.LoggedUserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class AccountApi(
    private val client: HttpClient,
    private val baseUrl: String = SfaConfig.baseUrl,
) {
    suspend fun loggedUser(token: String): SfaApiResult<AuthUser> = safeCall {
        val response = client.get("$baseUrl/v2/usuarios/logado") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.isSuccess()) {
            val dto = response.body<SfaObjectEnvelope<LoggedUserDto>>().data
                ?: return@safeCall SfaApiResult.Fail("empty_user", "Usuário não encontrado", response.status.value)
            SfaApiResult.Ok(
                AuthUser(
                    id = dto.id,
                    name = dto.nome,
                    email = dto.emailLogin,
                    avatarUrl = dto.avatarUrl,
                    whatsapp = dto.whatsapp,
                ),
            )
        } else {
            fail(response.status.value)
        }
    }

    suspend fun userCompanyIds(token: String, userId: Long): SfaApiResult<Set<Long>> = safeCall {
        val ids = mutableSetOf<Long>()
        var page = 1
        while (page <= MAX_PAGES) {
            val response = client.get("$baseUrl/v2/empresa-usuarios") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("page", page)
                parameter("per_page", LINK_PER_PAGE)
            }
            if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
            val env = response.body<SfaListEnvelope<EmpresaUsuarioDto>>()
            env.data.orEmpty().forEach { row ->
                if (row.usuarioId == userId && row.empresaId != null) ids.add(row.empresaId)
            }
            val pag = env.pagination
            if (pag == null || pag.currentPage >= pag.lastPage) break
            page++
        }
        SfaApiResult.Ok(ids)
    }

    suspend fun companies(token: String): SfaApiResult<List<Company>> = safeCall {
        val companies = mutableListOf<Company>()
        var page = 1
        while (page <= MAX_PAGES) {
            val response = client.get("$baseUrl/empresas") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("page", page)
                parameter("per_page", COMPANY_PER_PAGE)
            }
            if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
            val env = response.body<SfaListEnvelope<CompanyDto>>()
            env.data.orEmpty().forEach { dto ->
                companies.add(
                    Company(
                        id = dto.id,
                        name = dto.nomeFantasia ?: dto.razaoSocial ?: "Empresa ${dto.id}",
                        legalName = dto.razaoSocial,
                        cnpj = dto.cnpj,
                        logoUrl = dto.imagemLogotipo,
                    ),
                )
            }
            val pag = env.pagination
            if (pag == null || pag.currentPage >= pag.lastPage) break
            page++
        }
        SfaApiResult.Ok(companies)
    }

    private inline fun <T> safeCall(block: () -> SfaApiResult<T>): SfaApiResult<T> =
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }

    private fun fail(status: Int): SfaApiResult.Fail =
        SfaApiResult.Fail("http_$status", "HTTP $status", status)

    private companion object {
        const val MAX_PAGES = 40
        const val LINK_PER_PAGE = 500
        const val COMPANY_PER_PAGE = 200
    }
}
