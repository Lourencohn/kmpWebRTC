package app.trovata.cast.feature.dashboard

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.local.centsToBrl
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DashPeriod(val label: String, val days: Int, val bucketDays: Int) {
    Semana("Semana", 7, 1),
    Mes("Mês", 30, 3),
    Trimestre("Trimestre", 91, 7),
}

enum class DashMetric(val label: String) {
    Receita("Receita"),
    Pedidos("Pedidos"),
    Sessoes("Sessões"),
}

enum class DashTrend { Up, Flat, Down }

data class DashKpi(
    val label: String,
    val value: String,
    val delta: String? = null,
    val trend: DashTrend = DashTrend.Flat,
)

data class DashSeries(val values: List<Double>, val labels: List<String>)

data class DashTopProduct(
    val ref: String,
    val name: String,
    val units: Int,
    val revenueLabel: String,
    val fraction: Float,
)

data class DashboardUiState(
    val subtitle: String = "",
    val isLoading: Boolean = true,
    val hasData: Boolean = false,
    val period: DashPeriod = DashPeriod.Mes,
    val metric: DashMetric = DashMetric.Receita,
    val headlineLabel: String = "",
    val headlineValue: String = "",
    val headlineDelta: String? = null,
    val headlineTrend: DashTrend = DashTrend.Flat,
    val trend: DashSeries = DashSeries(emptyList(), emptyList()),
    val weekday: DashSeries = DashSeries(emptyList(), emptyList()),
    val kpis: List<DashKpi> = emptyList(),
    val topProducts: List<DashTopProduct> = emptyList(),
)

private data class DashControls(val period: DashPeriod, val metric: DashMetric)

private data class DashInputs(
    val orders: List<StoredOrder>,
    val sessions: List<StoredSessionRecord>,
    val clientCount: Long,
    val catalogCount: Long,
)

class DashboardScreenModel(
    private val orderRepository: OrderRepository,
    private val sessionsRepository: SessionsRepository,
    private val clientsRepository: ClientsRepository,
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ScreenModel {

    private val weekdayLabels = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")

    private val _controls = MutableStateFlow(DashControls(DashPeriod.Mes, DashMetric.Receita))
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        val inputs = combine(
            orderRepository.observeAll(),
            sessionsRepository.observeAll(),
            clientsRepository.observeCount(),
            catalogRepository.observeCount(),
        ) { orders, sessions, clientCount, catalogCount ->
            DashInputs(orders, sessions, clientCount, catalogCount)
        }

        screenModelScope.launch {
            combine(inputs, authRepository.activeCompany, _controls) { data, company, controls ->
                compute(data, company?.name.orEmpty(), controls)
            }.collect { _state.value = it }
        }
    }

    fun setPeriod(period: DashPeriod) = _controls.update { it.copy(period = period) }

    fun setMetric(metric: DashMetric) = _controls.update { it.copy(metric = metric) }

    private suspend fun compute(
        data: DashInputs,
        subtitle: String,
        controls: DashControls,
    ): DashboardUiState {
        val period = controls.period
        val metric = controls.metric
        val today = Instant.fromEpochMilliseconds(now()).toLocalDateTime(timeZone).date
        val windowStart = today.minus(DatePeriod(days = period.days - 1))
        val prevStart = windowStart.minus(DatePeriod(days = period.days))
        val windowStartMs = windowStart.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endMs = today.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toEpochMilliseconds()
        val prevStartMs = prevStart.atStartOfDayIn(timeZone).toEpochMilliseconds()

        val windowOrders = data.orders.filter { it.createdAtMs in windowStartMs until endMs }
        val prevOrders = data.orders.filter { it.createdAtMs in prevStartMs until windowStartMs }
        val windowSessions = data.sessions.filter { it.createdAtMs in windowStartMs until endMs }
        val prevSessions = data.sessions.filter { it.createdAtMs in prevStartMs until windowStartMs }

        val bucketCount = (period.days + period.bucketDays - 1) / period.bucketDays
        val trendValues = DoubleArray(bucketCount)
        val baseEpochDay = windowStart.toEpochDays()

        fun bucketOf(epochMs: Long): Int {
            val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
            val offset = date.toEpochDays() - baseEpochDay
            return (offset / period.bucketDays).coerceIn(0, bucketCount - 1)
        }

        when (metric) {
            DashMetric.Receita -> windowOrders.forEach { trendValues[bucketOf(it.createdAtMs)] += it.totalCents / 100.0 }
            DashMetric.Pedidos -> windowOrders.forEach { trendValues[bucketOf(it.createdAtMs)] += 1.0 }
            DashMetric.Sessoes -> windowSessions.forEach { trendValues[bucketOf(it.createdAtMs)] += 1.0 }
        }

        val trendLabels = (0 until bucketCount).map { index ->
            val date = windowStart.plus(DatePeriod(days = index * period.bucketDays))
            if (period == DashPeriod.Semana) weekdayLabels[date.dayOfWeek.isoDayNumber - 1]
            else "${date.dayOfMonth}/${date.monthNumber}"
        }

        val weekdayValues = DoubleArray(7)
        windowOrders.forEach { order ->
            val date = Instant.fromEpochMilliseconds(order.createdAtMs).toLocalDateTime(timeZone).date
            weekdayValues[date.dayOfWeek.isoDayNumber - 1] += 1.0
        }

        val revenueCents = windowOrders.sumOf { it.totalCents }
        val prevRevenueCents = prevOrders.sumOf { it.totalCents }
        val ordersCount = windowOrders.size
        val sessionsCount = windowSessions.size
        val units = windowOrders.sumOf { order -> order.lines.sumOf { it.units } }
        val ticketCents = if (ordersCount > 0) revenueCents / ordersCount else 0L
        val conversion = if (sessionsCount > 0) (ordersCount * 100.0 / sessionsCount).roundToInt() else null

        val unitsByRef = HashMap<String, Int>()
        val revenueByRef = HashMap<String, Long>()
        windowOrders.forEach { order ->
            order.lines.forEach { line ->
                unitsByRef[line.productId] = (unitsByRef[line.productId] ?: 0) + line.units
                revenueByRef[line.productId] = (revenueByRef[line.productId] ?: 0L) + line.subtotalCents
            }
        }
        val topRefs = unitsByRef.entries.sortedByDescending { it.value }.take(5).map { it.key }
        val names = catalogRepository.snapshotForRefs(topRefs).associate { it.ref to it.name }
        val maxUnits = (topRefs.maxOfOrNull { unitsByRef[it] ?: 0 } ?: 1).coerceAtLeast(1)
        val topProducts = topRefs.map { ref ->
            val refUnits = unitsByRef[ref] ?: 0
            DashTopProduct(
                ref = ref,
                name = names[ref] ?: ref,
                units = refUnits,
                revenueLabel = centsToBrl(revenueByRef[ref] ?: 0L),
                fraction = refUnits.toFloat() / maxUnits,
            )
        }

        val headlineValue: String
        val headlineDelta: String?
        val headlineTrend: DashTrend
        when (metric) {
            DashMetric.Receita -> {
                headlineValue = brlShort(revenueCents)
                headlineDelta = deltaLabel(revenueCents.toDouble(), prevRevenueCents.toDouble())
                headlineTrend = trendOf(revenueCents.toDouble(), prevRevenueCents.toDouble())
            }
            DashMetric.Pedidos -> {
                headlineValue = ordersCount.toString()
                headlineDelta = deltaLabel(ordersCount.toDouble(), prevOrders.size.toDouble())
                headlineTrend = trendOf(ordersCount.toDouble(), prevOrders.size.toDouble())
            }
            DashMetric.Sessoes -> {
                headlineValue = sessionsCount.toString()
                headlineDelta = deltaLabel(sessionsCount.toDouble(), prevSessions.size.toDouble())
                headlineTrend = trendOf(sessionsCount.toDouble(), prevSessions.size.toDouble())
            }
        }

        val kpis = listOf(
            DashKpi("Pedidos", ordersCount.toString(), deltaLabel(ordersCount.toDouble(), prevOrders.size.toDouble()), trendOf(ordersCount.toDouble(), prevOrders.size.toDouble())),
            DashKpi("Ticket médio", if (ordersCount > 0) centsToBrl(ticketCents) else "—"),
            DashKpi("Conversão", conversion?.let { "$it%" } ?: "—"),
            DashKpi("Sessões", sessionsCount.toString(), deltaLabel(sessionsCount.toDouble(), prevSessions.size.toDouble()), trendOf(sessionsCount.toDouble(), prevSessions.size.toDouble())),
            DashKpi("Itens vendidos", units.toString()),
            DashKpi("Clientes ativos", data.clientCount.toString()),
        )

        return DashboardUiState(
            subtitle = subtitle,
            isLoading = false,
            hasData = data.orders.isNotEmpty() || data.sessions.isNotEmpty(),
            period = period,
            metric = metric,
            headlineLabel = "${metric.label} · ${period.label}".uppercase(),
            headlineValue = headlineValue,
            headlineDelta = headlineDelta,
            headlineTrend = headlineTrend,
            trend = DashSeries(trendValues.toList(), trendLabels),
            weekday = DashSeries(weekdayValues.toList(), weekdayLabels),
            kpis = kpis,
            topProducts = topProducts,
        )
    }

    private fun trendOf(current: Double, previous: Double): DashTrend = when {
        previous <= 0.0 -> if (current > 0.0) DashTrend.Up else DashTrend.Flat
        current > previous * 1.005 -> DashTrend.Up
        current < previous * 0.995 -> DashTrend.Down
        else -> DashTrend.Flat
    }

    private fun deltaLabel(current: Double, previous: Double): String? {
        if (previous <= 0.0) return null
        val pct = ((current - previous) / previous * 100).roundToInt()
        val arrow = if (pct >= 0) "↑" else "↓"
        val sign = if (pct >= 0) "+" else "−"
        return "$arrow $sign${abs(pct)}% vs período anterior"
    }

    private fun brlShort(cents: Long): String {
        val reais = cents / 100.0
        return when {
            reais >= 1_000_000 -> "R$ ${oneDecimal(reais / 1_000_000)}mi"
            reais >= 1_000 -> "R$ ${oneDecimal(reais / 1_000)}k"
            else -> centsToBrl(cents)
        }
    }

    private fun oneDecimal(value: Double): String {
        val scaled = (value * 10).roundToInt()
        return "${scaled / 10},${scaled % 10}"
    }
}
