package app.trovata.cast.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.local.LinkedOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.feature.carts.idleTimeLabel
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.components.avatarHueFor
import app.trovata.cast.ui.format.durationLabel
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.format.hourMinuteAt
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.datetime.TimeZone

private const val CartItemsPreview = 8
private const val HistoryLimit = 6

@Composable
internal fun HeroCard(state: AppointmentDetailUiState, appointment: Appointment, actions: DetailActions) {
    val colors = TrovataTokens.colors
    val status = appointment.agendaStatus(state.nowMs)
    val today = localDateAt(state.nowMs, state.timeZone)
    val day = localDateAt(appointment.startsAtMs, state.timeZone)
    TrovataCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(name = appointment.clientName, hue = avatarHueFor(appointment.clientName), size = 46.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.clientName,
                        color = colors.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.01).em,
                    )
                    appointment.clientShop?.let { Text(text = it, color = colors.ink3, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = TrovataIcons.clock, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(16.dp))
                Text(
                    text = "${relativeDayLabel(day, today)} · ${hourMinuteAt(appointment.startsAtMs, state.timeZone)} às " +
                        "${hourMinuteAt(appointment.endsAtMs, state.timeZone)} · ${durationLabel(appointment.durationMinutes)}",
                    color = colors.ink2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (status == AgendaStatus.Upcoming) Pill(text = status.label, tone = PillTone.Brand, icon = TrovataIcons.calendar)
                StatusPill(status = status, appointment = appointment)
                if (appointment.isPriority) Pill(text = "Prioritário", tone = PillTone.Warn, icon = TrovataIcons.star)
                if (appointment.rescheduleCount > 0) {
                    Pill(text = "Reagendada ${appointment.rescheduleCount}x", tone = PillTone.Neutral)
                }
            }
            startLabel(appointment, state.nowMs)?.let {
                Text(text = it.replaceFirstChar { c -> c.uppercase() }, color = status.accent(), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
            appointment.linkedOrder?.let { order ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.jadeTint, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = TrovataIcons.check, contentDescription = null, tint = colors.jade2, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Pedido ${order.orderId} fechado nesta chamada · ${formatBrl(order.totalCents)}",
                        color = colors.jade2,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (appointment.canReschedule) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(text = "Reagendar", onClick = actions.onReschedule, kind = BtnKind.Ghost, size = BtnSize.Sm, icon = TrovataIcons.calendar)
                    if (state.callableCart != null) {
                        Btn(text = "Outro catálogo", onClick = { actions.onInvite(appointment) }, kind = BtnKind.Ghost, size = BtnSize.Sm, icon = TrovataIcons.grid)
                    }
                    Text(
                        text = "Cancelar",
                        color = colors.ink3,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f).clickable(onClick = actions.onRequestCancel).padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ObjectiveCard(appointment: Appointment, onEdit: () -> Unit) {
    val colors = TrovataTokens.colors
    val editable = appointment.status == AppointmentStatus.Scheduled
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Objetivo")
        TrovataCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val objective = appointment.objective
                if (objective != null) {
                    Text(text = objective, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Sem objetivo definido", color = colors.ink3, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                        if (editable) {
                            Text(
                                text = "Definir",
                                color = colors.brand,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(onClick = onEdit).padding(6.dp),
                            )
                        }
                    }
                }
                appointment.notes?.let { notes ->
                    AgendaDivider()
                    Text(text = "PREPARAÇÃO", color = colors.ink4, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em)
                    Text(text = notes, color = colors.ink2, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
internal fun PrepCard(state: AppointmentDetailUiState, appointment: Appointment, onToggle: (PrepTask) -> Unit) {
    val colors = TrovataTokens.colors
    val tasks = state.prepTasks
    val done = tasks.count { appointment.isPrepDone(it) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Antes de chamar",
            action = { Pill(text = "$done de ${tasks.size}", tone = if (done == tasks.size) PillTone.Jade else PillTone.Neutral) },
        )
        TrovataCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.lastContact?.let { last ->
                    LastContactNote(last = last, timeZone = state.timeZone)
                }
                state.readiness.forEach { ReadinessRow(it) }
                AgendaDivider()
                tasks.forEach { task ->
                    CheckRow(label = task.label, checked = appointment.isPrepDone(task), onToggle = { onToggle(task) })
                }
            }
        }
    }
}

@Composable
private fun LastContactNote(last: Appointment, timeZone: TimeZone) {
    val colors = TrovataTokens.colors
    val date = localDateAt(last.startsAtMs, timeZone)
    val headline = "Última conversa em ${dayMonthLabel(date)}: ${last.outcome?.label() ?: last.agendaStatus(last.endsAtMs).label}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(colors.brandTint, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = headline, color = colors.brand2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        last.outcomeNotes?.let { Text(text = "“$it”", color = colors.ink2, fontSize = 12.5.sp, fontStyle = FontStyle.Italic) }
        last.nextStep?.let { Text(text = "Ficou combinado: $it", color = colors.ink2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
internal fun ResultCard(appointment: Appointment) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Resultado")
        TrovataCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val label = when (appointment.status) {
                    AppointmentStatus.Missed -> AgendaStatus.Missed.label
                    AppointmentStatus.Cancelled -> AgendaStatus.Cancelled.label
                    else -> appointment.outcome?.label() ?: AgendaStatus.Done.label
                }
                Text(text = label, color = colors.ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                appointment.outcomeNotes?.let { Text(text = it, color = colors.ink2, fontSize = 13.sp, lineHeight = 18.sp) }
                appointment.nextStep?.let {
                    Text(text = "Próximo passo: $it", color = colors.brand2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun CartsCard(state: AppointmentDetailUiState, appointment: Appointment, actions: DetailActions) {
    val colors = TrovataTokens.colors
    val carts = state.openCarts
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Carrinhos em aberto",
            action = if (carts.isNotEmpty()) ({ Pill(text = carts.size.toString(), tone = PillTone.Brand, icon = TrovataIcons.cart) }) else null,
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                when {
                    state.cartsLoading -> Text(
                        text = "Buscando carrinhos desse cliente...",
                        color = colors.ink3,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                    state.cartsError != null -> Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(text = "Sem conexão com o Catálogo Link agora.", color = colors.ink3, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                        Btn(text = "Tentar de novo", onClick = actions.onRetryCarts, kind = BtnKind.Surface, size = BtnSize.Sm)
                    }
                    carts.isEmpty() -> Text(
                        text = "Nenhum carrinho aberto desse cliente nos seus catálogos.",
                        color = colors.ink3,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                    else -> carts.forEachIndexed { index, cart ->
                        CartBlock(
                            cart = cart,
                            state = state,
                            canCall = appointment.canCall,
                            onToggle = { actions.onToggleCart(cart) },
                            onCall = { actions.onCallWithCart(cart) },
                        )
                        if (index < carts.lastIndex) AgendaDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CartBlock(
    cart: OpenCart,
    state: AppointmentDetailUiState,
    canCall: Boolean,
    onToggle: () -> Unit,
    onCall: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val expanded = state.expandedCartId == cart.carrinhoId
    val blockReason = cart.callBlockReason(state.nowMs)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Esconder itens" else "Ver itens", onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cart.catalogName ?: "Carrinho ${cart.carrinhoId}",
                    color = colors.ink,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        "${cart.itemCount} ${if (cart.itemCount == 1) "item" else "itens"}",
                        "${cart.totalQuantity} un",
                        idleTimeLabel(cart.updatedAtMs, state.nowMs),
                    ).joinToString(" · "),
                    color = colors.ink3,
                    fontSize = 12.sp,
                )
            }
            cart.totalCents?.let {
                Text(text = formatBrl(it), style = TrovataTokens.type.mono.copy(fontSize = 13.5.sp, color = colors.ink, fontWeight = FontWeight.SemiBold))
            }
            Icon(
                imageVector = if (expanded) TrovataIcons.chevDown else TrovataIcons.chev,
                contentDescription = null,
                tint = colors.ink4,
                modifier = Modifier.size(16.dp),
            )
        }
        if (expanded) CartItems(items = state.cartItems[cart.carrinhoId], loading = state.loadingItemsFor == cart.carrinhoId)
        when {
            !canCall -> Unit
            blockReason == null -> Btn(
                text = if (state.callingCartId == cart.carrinhoId) "Abrindo sessão..." else "Chamar com este carrinho",
                onClick = onCall,
                kind = BtnKind.Jade,
                size = BtnSize.Sm,
                icon = TrovataIcons.video,
                enabled = state.callingCartId == null,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> Text(text = blockReason, color = colors.ink3, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun CartItems(items: List<CartItemLine>?, loading: Boolean) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface2, shape)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            loading || items == null -> Text(text = "Carregando itens...", color = colors.ink3, fontSize = 12.sp)
            items.isEmpty() -> Text(text = "Carrinho sem itens.", color = colors.ink3, fontSize = 12.sp)
            else -> {
                items.take(CartItemsPreview).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = listOfNotNull(line.name, line.color).joinToString(" · "),
                                color = colors.ink,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (line.sizes.isNotEmpty()) {
                                Text(
                                    text = line.sizes.joinToString("  ") { "${it.label} ${it.quantity}" },
                                    color = colors.ink4,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(text = "${line.quantity} un", color = colors.ink3, fontSize = 12.sp)
                        line.totalCents?.let {
                            Text(text = formatBrl(it), style = TrovataTokens.type.mono.copy(fontSize = 12.sp, color = colors.ink2))
                        }
                    }
                }
                if (items.size > CartItemsPreview) {
                    Text(text = "e mais ${items.size - CartItemsPreview} produtos", color = colors.ink4, fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
internal fun OrdersInProgressCard(carts: List<OpenCart>, nowMs: Long) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Pedidos em andamento")
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                carts.forEachIndexed { index, cart ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cart.catalogName ?: "Carrinho ${cart.carrinhoId}", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${cart.status.label} · ${idleTimeLabel(cart.updatedAtMs, nowMs).replace("sem mexer", "atualizado")}",
                                color = colors.ink4,
                                fontSize = 11.5.sp,
                            )
                        }
                        cart.totalCents?.let {
                            Text(text = formatBrl(it), style = TrovataTokens.type.mono.copy(fontSize = 13.sp, color = colors.ink, fontWeight = FontWeight.SemiBold))
                        }
                    }
                    if (index < carts.lastIndex) AgendaDivider()
                }
            }
        }
    }
}

private sealed interface HistoryEntry {
    val atMs: Long

    data class Call(val appointment: Appointment) : HistoryEntry {
        override val atMs: Long get() = appointment.startsAtMs
    }

    data class Session(val session: StoredSessionRecord) : HistoryEntry {
        override val atMs: Long get() = session.createdAtMs
    }
}

@Composable
internal fun HistoryCard(state: AppointmentDetailUiState) {
    val colors = TrovataTokens.colors
    val entries = (state.history.map { HistoryEntry.Call(it) } + state.unlinkedSessions.map { HistoryEntry.Session(it) })
        .sortedByDescending { it.atMs }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Histórico com o cliente")
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                if (entries.isEmpty()) {
                    Text(
                        text = "Nenhuma chamada ou sessão anterior registrada neste aparelho.",
                        color = colors.ink3,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
                entries.take(HistoryLimit).forEachIndexed { index, entry ->
                    when (entry) {
                        is HistoryEntry.Call -> HistoryCallRow(entry.appointment, state)
                        is HistoryEntry.Session -> HistorySessionRow(entry.session, state)
                    }
                    if (index < minOf(entries.size, HistoryLimit) - 1) AgendaDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryCallRow(appointment: Appointment, state: AppointmentDetailUiState) {
    val colors = TrovataTokens.colors
    val status = appointment.agendaStatus(state.nowMs)
    val order = appointment.linkedOrder ?: state.orderFor(appointment.sessionId)?.let {
        LinkedOrder(it.orderId, it.totalCents)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(54.dp)) {
            Text(text = dayMonthLabel(localDateAt(appointment.startsAtMs, state.timeZone)), style = TrovataTokens.type.mono.copy(fontSize = 12.5.sp, color = colors.ink, fontWeight = FontWeight.SemiBold))
            Text(text = hourMinuteAt(appointment.startsAtMs, state.timeZone), color = colors.ink4, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (status == AgendaStatus.Upcoming) Pill(text = status.label, tone = PillTone.Brand) else StatusPill(status = status, appointment = appointment)
            }
            appointment.objective?.let { Text(text = it, color = colors.ink2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) }
            appointment.outcomeNotes?.let { Text(text = "“$it”", color = colors.ink3, fontSize = 12.sp, fontStyle = FontStyle.Italic) }
            appointment.nextStep?.let { Text(text = "Próximo passo: $it", color = colors.ink3, fontSize = 12.sp) }
            order?.let { Text(text = "Pedido ${it.orderId} · ${formatBrl(it.totalCents)}", color = colors.jade2, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun HistorySessionRow(session: StoredSessionRecord, state: AppointmentDetailUiState) {
    val colors = TrovataTokens.colors
    val order = state.orderFor(session.sessionId)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(54.dp)) {
            Text(text = dayMonthLabel(localDateAt(session.createdAtMs, state.timeZone)), style = TrovataTokens.type.mono.copy(fontSize = 12.5.sp, color = colors.ink, fontWeight = FontWeight.SemiBold))
            Text(text = hourMinuteAt(session.createdAtMs, state.timeZone), color = colors.ink4, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = "Sessão ao vivo", color = colors.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text(text = session.catalogLabel.ifBlank { "Catálogo ${session.catalogoUuid.take(8)}" }, color = colors.ink3, fontSize = 12.sp)
            order?.let { Text(text = "Pedido ${it.orderId} · ${formatBrl(it.totalCents)}", color = colors.jade2, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
internal fun ClientCard(state: AppointmentDetailUiState, appointment: Appointment) {
    val colors = TrovataTokens.colors
    val client = state.client
    val rows = listOfNotNull(
        (client?.legalName ?: appointment.clientShop)?.let { "Razão social" to it },
        (client?.document ?: appointment.clientDocument)?.let { "CNPJ/CPF" to it },
        client?.contact?.let { "Contato" to it },
        (client?.phone ?: appointment.clientPhone)?.let { "Telefone" to it },
        (client?.email ?: appointment.clientEmail)?.let { "E-mail" to it },
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Cliente",
            action = if (client != null && !client.active) ({ Pill(text = "Inativo", tone = PillTone.Neutral) }) else null,
        )
        TrovataCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (label, value) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = label, color = colors.ink4, fontSize = 12.sp, modifier = Modifier.width(92.dp))
                        Text(text = value, color = colors.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
                if (appointment.clienteId == null) {
                    Text(
                        text = "Cliente sem cadastro no SFA. Os dados vieram do carrinho.",
                        color = colors.ink4,
                        fontSize = 11.5.sp,
                    )
                }
            }
        }
    }
}
