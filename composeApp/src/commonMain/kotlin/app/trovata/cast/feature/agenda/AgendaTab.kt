package app.trovata.cast.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.feature.carts.idleTimeLabel
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.format.hourMinute
import app.trovata.cast.ui.format.hourMinuteAt
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.datetime.LocalDate

class AgendaActions(
    val onNewAppointment: (date: LocalDate?, minuteOfDay: Int?) -> Unit,
    val onOpenAppointment: (Appointment) -> Unit,
    val onScheduleFollowUp: (Appointment) -> Unit,
    val onScheduleCart: (OpenCart) -> Unit,
)

@Composable
fun AgendaTab(
    screenModel: AgendaScreenModel,
    accountName: String,
    onOpenAccount: () -> Unit,
    actions: AgendaActions,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()
    AgendaBody(
        state = state,
        accountName = accountName,
        onOpenAccount = onOpenAccount,
        actions = actions,
        onView = screenModel::selectView,
        onSelectDate = screenModel::selectDate,
        onPrevious = screenModel::previousPeriod,
        onNext = screenModel::nextPeriod,
        onToday = screenModel::goToToday,
        onRetryCarts = screenModel::refreshCarts,
        modifier = modifier,
    )
}

@Composable
fun AgendaBody(
    state: AgendaUiState,
    accountName: String,
    onOpenAccount: () -> Unit,
    actions: AgendaActions,
    onView: (AgendaView) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onRetryCarts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val newOnSelected = { actions.onNewAppointment(state.selectedDate.takeIf { it >= state.today }, null) }
    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = "Chamadas com clientes",
                    title = "Agenda",
                    subtitle = state.subtitle,
                    accountName = accountName,
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.plus,
                    onSecondaryClick = newOnSelected,
                )
            }

            if (!state.hasOwner) {
                item { NoCompanyCard() }
                return@LazyColumn
            }

            item { SummaryTiles(state.summary) }

            item {
                PeriodControls(
                    view = state.view,
                    periodLabel = state.periodLabel,
                    isShowingToday = state.isShowingToday,
                    onView = onView,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToday = onToday,
                )
            }

            item {
                when (state.view) {
                    AgendaView.Month -> MonthGrid(
                        days = state.monthGrid,
                        month = state.selectedDate,
                        selected = state.selectedDate,
                        today = state.today,
                        loadOf = state::loadOf,
                        onSelect = onSelectDate,
                    )
                    else -> WeekStrip(
                        week = state.week,
                        selected = state.selectedDate.takeIf { state.view == AgendaView.Day },
                        today = state.today,
                        loadOf = state::loadOf,
                        onSelect = onSelectDate,
                    )
                }
            }

            when (state.view) {
                AgendaView.Week -> weekItems(state, actions)
                AgendaView.Day -> dayItems(state, actions, showGaps = true)
                AgendaView.Month -> dayItems(state, actions, showGaps = false)
            }

            if (state.pending.isNotEmpty() || state.followUps.isNotEmpty()) {
                item { AttentionSection(state, actions) }
            }

            item { CartsToCallSection(state = state, onSchedule = actions.onScheduleCart, onRetry = onRetryCarts) }

            item { LocalStorageNote() }
        }
    }
}

private fun LazyListScope.weekItems(state: AgendaUiState, actions: AgendaActions) {
    state.week.forEach { day ->
        item(key = "day-${day.toEpochDays()}") {
            val appointments = state.appointmentsOn(day)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayHeader(
                    label = relativeDayLabel(day, state.today),
                    count = appointments.count { it.agendaStatus(state.nowMs) != AgendaStatus.Cancelled },
                    highlight = day == state.today,
                    onAdd = if (day >= state.today) ({ actions.onNewAppointment(day, null) }) else null,
                )
                appointments.forEach { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        nowMs = state.nowMs,
                        timeZone = state.timeZone,
                        openCart = state.openCartFor(appointment),
                        onOpen = { actions.onOpenAppointment(appointment) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.dayItems(state: AgendaUiState, actions: AgendaActions, showGaps: Boolean) {
    val day = state.selectedDate
    val appointments = state.appointmentsOn(day)
    item(key = "day-title") {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            DayHeader(
                label = if (state.view == AgendaView.Month) longDayLabel(day, state.today) else "Chamadas do dia",
                count = appointments.count { it.agendaStatus(state.nowMs) != AgendaStatus.Cancelled },
                highlight = false,
                onAdd = if (day >= state.today) ({ actions.onNewAppointment(day, null) }) else null,
            )
        }
    }
    if (appointments.isEmpty()) {
        item(key = "day-empty") {
            EmptyDayCard(
                canSchedule = day >= state.today,
                onSchedule = { actions.onNewAppointment(day, null) },
            )
        }
        return
    }
    val gaps = if (showGaps) freeGaps(day, appointments, state.nowMs, state.timeZone) else emptyList()
    val entries = appointments.map { DayEntry.Call(it) } + gaps.map { DayEntry.Gap(it) }
    entries.sortedBy { it.sortMinute(state) }.forEach { entry ->
        when (entry) {
            is DayEntry.Call -> item(key = "call-${entry.appointment.appointmentId}") {
                AppointmentCard(
                    appointment = entry.appointment,
                    nowMs = state.nowMs,
                    timeZone = state.timeZone,
                    openCart = state.openCartFor(entry.appointment),
                    onOpen = { actions.onOpenAppointment(entry.appointment) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            is DayEntry.Gap -> item(key = "gap-${entry.minutes.first}") {
                FreeGapRow(minutes = entry.minutes, onSchedule = { actions.onNewAppointment(day, entry.minutes.first) })
            }
        }
    }
}

private sealed interface DayEntry {
    data class Call(val appointment: Appointment) : DayEntry
    data class Gap(val minutes: IntRange) : DayEntry

    fun sortMinute(state: AgendaUiState): Int = when (this) {
        is Call -> minuteOfDayAt(appointment.startsAtMs, state.timeZone)
        is Gap -> minutes.first
    }
}

@Composable
private fun DayHeader(label: String, count: Int, highlight: Boolean, onAdd: (() -> Unit)?) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = if (highlight) colors.brand2 else colors.ink,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (count == 0) "livre" else plural(count, "chamada", "chamadas"),
            color = colors.ink4,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        if (onAdd != null) {
            Text(
                text = "+ Agendar",
                color = colors.brand,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAdd).padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FreeGapRow(minutes: IntRange, onSchedule: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    val end = minutes.last + 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onSchedule)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = TrovataIcons.clock, contentDescription = null, tint = colors.ink4, modifier = Modifier.size(15.dp))
        Text(
            text = "Livre das ${hourMinute(minutes.first / 60, minutes.first % 60)} às ${hourMinute(end / 60, end % 60)}",
            color = colors.ink3,
            fontSize = 12.5.sp,
            modifier = Modifier.weight(1f),
        )
        Text(text = "Agendar", color = colors.brand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryTiles(summary: AgendaSummary) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryTile(label = "Hoje", value = summary.today, color = colors.ink, modifier = Modifier.weight(1f))
        SummaryTile(label = "Feitas hoje", value = summary.doneToday, color = colors.jade2, modifier = Modifier.weight(1f))
        SummaryTile(
            label = "Pendentes",
            value = summary.pending,
            color = if (summary.pending > 0) colors.warn else colors.ink,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            label = "Retornos",
            value = summary.followUps,
            color = if (summary.followUps > 0) colors.brand2 else colors.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(text = value.toString(), color = color, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        Text(text = label, color = colors.ink4, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AttentionSection(state: AgendaUiState, actions: AgendaActions) {
    val colors = TrovataTokens.colors
    val total = state.pending.size + state.followUps.size
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Precisa de você",
            action = { Pill(text = total.toString(), tone = PillTone.Warn, icon = TrovataIcons.alert) },
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                val rows = state.pending.map { it to true } + state.followUps.map { it to false }
                rows.forEachIndexed { index, (appointment, isPending) ->
                    if (isPending) {
                        AttentionRow(
                            title = appointment.clientName,
                            detail = "${relativeDayLabel(localDateAt(appointment.startsAtMs, state.timeZone), state.today)} às " +
                                "${hourMinuteAt(appointment.startsAtMs, state.timeZone)} · registre o resultado ou reagende",
                            icon = TrovataIcons.alert,
                            iconTint = colors.warn,
                            onClick = { actions.onOpenAppointment(appointment) },
                        )
                    } else {
                        AttentionRow(
                            title = appointment.clientName,
                            detail = "Combinado: ${appointment.nextStep ?: "retornar o contato"}",
                            icon = TrovataIcons.refresh,
                            iconTint = colors.brand,
                            onClick = { actions.onOpenAppointment(appointment) },
                            action = {
                                Btn(
                                    text = "Agendar",
                                    onClick = { actions.onScheduleFollowUp(appointment) },
                                    kind = BtnKind.Soft,
                                    size = BtnSize.Sm,
                                )
                            },
                        )
                    }
                    if (index < rows.lastIndex) AgendaDivider()
                }
            }
        }
    }
}

@Composable
private fun AttentionRow(
    title: String,
    detail: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = detail, color = colors.ink3, fontSize = 12.sp, lineHeight = 16.sp)
        }
        action?.invoke()
    }
}

@Composable
private fun CartsToCallSection(state: AgendaUiState, onSchedule: (OpenCart) -> Unit, onRetry: () -> Unit) {
    val colors = TrovataTokens.colors
    val carts = state.cartsToCall
    if (!state.cartsLoading && state.cartsError == null && carts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Carrinhos parados sem chamada",
            action = if (carts.isNotEmpty()) ({ Pill(text = carts.size.toString(), tone = PillTone.Brand, icon = TrovataIcons.cart) }) else null,
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                when {
                    state.cartsLoading -> Text(
                        text = "Buscando carrinhos no Catálogo Link...",
                        color = colors.ink3,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                    state.cartsError != null -> Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Não deu para buscar os carrinhos agora. ${state.cartsError}",
                            color = colors.ink3,
                            fontSize = 12.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Btn(text = "Tentar de novo", onClick = onRetry, kind = BtnKind.Surface, size = BtnSize.Sm)
                    }
                    else -> carts.forEachIndexed { index, cart ->
                        CartToCallRow(cart = cart, nowMs = state.nowMs, onSchedule = { onSchedule(cart) })
                        if (index < carts.lastIndex) AgendaDivider()
                    }
                }
            }
        }
        if (carts.isNotEmpty()) {
            Text(
                text = "Carrinhos com itens, mexidos entre 1 e 30 dias atrás, de clientes sem chamada marcada.",
                color = colors.ink4,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun CartToCallRow(cart: OpenCart, nowMs: Long, onSchedule: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = cart.title, color = colors.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOfNotNull(
                    cart.totalCents?.let { formatBrl(it) },
                    "${cart.itemCount} ${if (cart.itemCount == 1) "item" else "itens"}",
                    idleTimeLabel(cart.updatedAtMs, nowMs),
                ).joinToString(" · "),
                color = colors.ink3,
                fontSize = 12.sp,
            )
            cart.catalogName?.let { Text(text = it, color = colors.ink4, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Btn(text = "Agendar", onClick = onSchedule, kind = BtnKind.Soft, size = BtnSize.Sm, icon = TrovataIcons.calendar)
    }
}

@Composable
private fun EmptyDayCard(canSchedule: Boolean, onSchedule: () -> Unit) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Nenhuma chamada neste dia", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (canSchedule) {
                Text(
                    text = "Marque uma chamada e ela aparece aqui com o contexto do cliente pronto para você.",
                    color = colors.ink3,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
                Btn(text = "Agendar chamada", onClick = onSchedule, kind = BtnKind.Primary, size = BtnSize.Sm, icon = TrovataIcons.plus)
            }
        }
    }
}

@Composable
private fun NoCompanyCard() {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Selecione uma empresa na sua conta para ver e montar a agenda.",
            color = colors.ink3,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun LocalStorageNote() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(imageVector = TrovataIcons.lock, contentDescription = null, tint = colors.ink4, modifier = Modifier.size(14.dp))
        Text(
            text = "Sua agenda fica salva neste aparelho. Ela ainda não sincroniza com o SFA nem aparece em outro celular.",
            color = colors.ink4,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
    }
}
