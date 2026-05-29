package app.trovata.cast.feature.insights

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.local.centsToBrl
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
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

data class InsightKpi(val label: String, val value: String)

data class TopProduct(
    val ref: String,
    val name: String,
    val units: Int,
    val revenueCents: Long,
)

data class InsightsUiState(
    val subtitle: String = "",
    val hasData: Boolean = false,
    val revenueLabel: String = centsToBrl(0),
    val deltaLabel: String? = null,
    val spark: List<Float> = emptyList(),
    val kpis: List<InsightKpi> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
)

class InsightsScreenModel(
    private val orderRepository: OrderRepository,
    private val catalogRepository: CatalogRepository,
    private val sessionsRepository: SessionsRepository,
    private val authRepository: AuthRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ScreenModel {

    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init {
        val today = Instant.fromEpochMilliseconds(now()).toLocalDateTime(timeZone).date
        val monthStart = LocalDate(today.year, today.monthNumber, 1)
        val nextMonthStart = monthStart.plus(DatePeriod(months = 1))
        val prevMonthStart = monthStart.minus(DatePeriod(months = 1))
        val monthStartMs = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val nextMonthMs = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val prevMonthMs = prevMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val daysElapsed = today.dayOfMonth

        screenModelScope.launch {
            combine(
                orderRepository.observeBetween(monthStartMs, nextMonthMs),
                orderRepository.observeBetween(prevMonthMs, monthStartMs),
                sessionsRepository.observeAll(),
                authRepository.activeCompany,
            ) { monthOrders, prevOrders, sessions, company ->
                val revenueCents = monthOrders.sumOf { it.totalCents }
                val prevRevenue = prevOrders.sumOf { it.totalCents }
                val ordersCount = monthOrders.size
                val units = monthOrders.sumOf { order -> order.lines.sumOf { it.units } }
                val ticketCents = if (ordersCount > 0) revenueCents / ordersCount else 0L
                val sessionsThisMonth = sessions.count { it.createdAtMs in monthStartMs until nextMonthMs }

                val unitsByRef = HashMap<String, Int>()
                val revenueByRef = HashMap<String, Long>()
                monthOrders.forEach { order ->
                    order.lines.forEach { line ->
                        unitsByRef[line.productId] = (unitsByRef[line.productId] ?: 0) + line.units
                        revenueByRef[line.productId] = (revenueByRef[line.productId] ?: 0L) + line.subtotalCents
                    }
                }
                val topRefs = unitsByRef.entries.sortedByDescending { it.value }.take(5).map { it.key }
                val names = catalogRepository.snapshotForRefs(topRefs).associate { it.ref to it.name }
                val topProducts = topRefs.map { ref ->
                    TopProduct(
                        ref = ref,
                        name = names[ref] ?: ref,
                        units = unitsByRef[ref] ?: 0,
                        revenueCents = revenueByRef[ref] ?: 0L,
                    )
                }

                val daily = LongArray(daysElapsed.coerceAtLeast(1))
                monthOrders.forEach { order ->
                    val day = Instant.fromEpochMilliseconds(order.createdAtMs).toLocalDateTime(timeZone).dayOfMonth
                    val index = (day - 1).coerceIn(0, daily.lastIndex)
                    daily[index] = daily[index] + order.totalCents
                }

                InsightsUiState(
                    subtitle = company?.name.orEmpty(),
                    hasData = ordersCount > 0,
                    revenueLabel = centsToBrl(revenueCents),
                    deltaLabel = deltaLabel(revenueCents, prevRevenue),
                    spark = daily.map { (it / 100).toFloat() },
                    kpis = listOf(
                        InsightKpi("Pedidos", ordersCount.toString()),
                        InsightKpi("Ticket médio", centsToBrl(ticketCents)),
                        InsightKpi("Itens", units.toString()),
                        InsightKpi("Sessões", sessionsThisMonth.toString()),
                    ),
                    topProducts = topProducts,
                )
            }.collect { state -> _state.value = state }
        }
    }

    private fun deltaLabel(current: Long, previous: Long): String? {
        if (previous <= 0L) return null
        val pct = ((current - previous).toDouble() / previous * 100).roundToInt()
        val arrow = if (pct >= 0) "↑" else "↓"
        return "$arrow ${if (pct >= 0) "+" else "−"}${abs(pct)}% vs mês anterior"
    }
}
