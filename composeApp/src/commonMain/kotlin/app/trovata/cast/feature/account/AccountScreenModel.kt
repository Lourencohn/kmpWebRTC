package app.trovata.cast.feature.account

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.AuthUser
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.centsToBrl
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class AccountUiState(
    val displayName: String = "Minha conta",
    val role: String = "Representante",
    val email: String = "—",
    val brandName: String = "—",
    val brandSubtitle: String = "—",
    val accountRows: List<AccountRow> = emptyList(),
    val performance: List<AccountStat> = emptyList(),
    val support: List<SupportRow> = SampleAccount.support,
    val activeClients: Int = 0,
)

class AccountScreenModel(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val clientsRepository: ClientsRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ScreenModel {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        val today = Instant.fromEpochMilliseconds(now()).toLocalDateTime(timeZone).date
        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
        val monthStartMs = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val nextMonthMs = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()

        screenModelScope.launch {
            val clientCount = clientsRepository.count().toInt()
            combine(
                authRepository.user,
                authRepository.activeCompany,
                orderRepository.observeBetween(monthStartMs, nextMonthMs),
            ) { user, company, orders ->
                buildState(user, company, orders, clientCount)
            }.collect { _state.value = it }
        }
    }

    private fun buildState(
        user: AuthUser?,
        company: Company?,
        monthOrders: List<StoredOrder>,
        clientCount: Int,
    ): AccountUiState {
        val name = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Minha conta"
        val email = user?.email ?: "—"
        val brandName = company?.name ?: "—"
        val brandSubtitle = company?.legalName?.takeIf { it.isNotBlank() } ?: brandName
        val revenueCents = monthOrders.sumOf { it.totalCents }
        val units = monthOrders.sumOf { order -> order.lines.sumOf { it.units } }
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
                AccountStat("Vendas", centsToBrl(revenueCents), "", true),
                AccountStat("Pedidos", monthOrders.size.toString(), "", true),
                AccountStat("Itens", units.toString(), "", true),
            ),
            support = SampleAccount.support,
            activeClients = clientCount,
        )
    }
}
