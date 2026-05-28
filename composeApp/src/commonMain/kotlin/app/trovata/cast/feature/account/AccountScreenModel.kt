package app.trovata.cast.feature.account

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.AuthUser
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.sample.AccountRow
import app.trovata.cast.data.sample.AccountStat
import app.trovata.cast.data.sample.SampleAccount
import app.trovata.cast.data.sample.SupportRow
import app.trovata.cast.ui.icons.TrovataIcons
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AccountUiState(
    val displayName: String,
    val role: String,
    val email: String,
    val brandName: String,
    val brandSubtitle: String,
    val accountRows: List<AccountRow>,
    val performance: List<AccountStat>,
    val support: List<SupportRow>,
    val activeClients: Int,
    val monthsOnNetwork: Int,
    val tierLabel: String,
)

class AccountScreenModel(authRepository: AuthRepository) : ScreenModel {

    private val _state = MutableStateFlow(
        buildState(authRepository.user.value, authRepository.activeCompany.value),
    )
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            combine(authRepository.user, authRepository.activeCompany) { user, company ->
                buildState(user, company)
            }.collect { _state.value = it }
        }
    }

    private fun buildState(user: AuthUser?, company: Company?): AccountUiState {
        val name = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Minha conta"
        val email = user?.email ?: "—"
        val brandName = company?.name ?: "—"
        val brandSubtitle = company?.legalName?.takeIf { it.isNotBlank() } ?: brandName
        return AccountUiState(
            displayName = name,
            role = company?.name?.let { "Representante · $it" } ?: "Representante",
            email = email,
            brandName = brandName,
            brandSubtitle = brandSubtitle,
            accountRows = buildList {
                add(AccountRow("Perfil", "$name · $email", TrovataIcons.user))
                add(AccountRow("Empresa representada", brandName, TrovataIcons.layers))
                addAll(SampleAccount.account.drop(2))
            },
            performance = SampleAccount.performance,
            support = SampleAccount.support,
            activeClients = SampleAccount.activeClients,
            monthsOnNetwork = SampleAccount.monthsOnNetwork,
            tierLabel = SampleAccount.tierLabel,
        )
    }
}
