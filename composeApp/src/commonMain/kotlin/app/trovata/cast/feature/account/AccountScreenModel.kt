package app.trovata.cast.feature.account

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.AuthUser
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.ui.icons.TrovataIcons
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val supportRows = listOf(
    SupportRow("Central de ajuda", TrovataIcons.msg),
    SupportRow("Termos de uso", TrovataIcons.lock),
    SupportRow("Política de privacidade", TrovataIcons.eye),
    SupportRow("Versão", TrovataIcons.star, value = "0.1.0"),
)

data class AccountUiState(
    val displayName: String = "Minha conta",
    val role: String = "Representante",
    val email: String = "—",
    val brandName: String = "—",
    val brandSubtitle: String = "—",
    val accountRows: List<AccountRow> = emptyList(),
    val performance: List<AccountStat> = emptyList(),
    val support: List<SupportRow> = supportRows,
    val activeClients: Int = 0,
)

class AccountScreenModel(
    private val authRepository: AuthRepository,
    private val sessionsRepository: SessionsRepository,
    private val clientsRepository: ClientsRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            val clientCount = clientsRepository.count().toInt()
            combine(
                authRepository.user,
                authRepository.activeCompany,
                sessionsRepository.observeAll(),
            ) { user, company, sessions ->
                buildState(user, company, sessions, clientCount)
            }.collect { _state.value = it }
        }
    }

    private fun buildState(
        user: AuthUser?,
        company: Company?,
        sessions: List<StoredSessionRecord>,
        clientCount: Int,
    ): AccountUiState {
        val name = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Minha conta"
        val email = user?.email ?: "—"
        val brandName = company?.name ?: "—"
        val brandSubtitle = company?.legalName?.takeIf { it.isNotBlank() } ?: brandName
        val catalogos = sessions.map { it.catalogoUuid }.distinct().size
        return AccountUiState(
            displayName = name,
            role = company?.name?.let { "Representante · $it" } ?: "Representante",
            email = email,
            brandName = brandName,
            brandSubtitle = brandSubtitle,
            accountRows = buildList {
                add(AccountRow("Perfil", "$name · $email", TrovataIcons.user))
                add(AccountRow("Empresa representada", brandName, TrovataIcons.layers))
                company?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    add(AccountRow("CNPJ", it, TrovataIcons.grid))
                }
            },
            performance = listOf(
                AccountStat("Sessões", sessions.size.toString(), "", true),
                AccountStat("Catálogos", catalogos.toString(), "", true),
                AccountStat("Clientes", clientCount.toString(), "", true),
            ),
            support = supportRows,
            activeClients = clientCount,
        )
    }
}
