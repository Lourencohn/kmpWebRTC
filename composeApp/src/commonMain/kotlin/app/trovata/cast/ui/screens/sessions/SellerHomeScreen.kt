package app.trovata.cast.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.sample.HistorySession
import app.trovata.cast.data.sample.HistoryStatus
import app.trovata.cast.data.sample.LiveWaitingSession
import app.trovata.cast.data.sample.SessionTag
import app.trovata.cast.data.sample.UpcomingSession
import app.trovata.cast.feature.sessions.SessionsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun SellerHomeScreen(
    viewModel: SessionsViewModel,
    modifier: Modifier = Modifier,
    onOpenIncoming: (LiveWaitingSession) -> Unit = {},
    onOpenPrep: (UpcomingSession) -> Unit = {},
    onInviteClient: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
) {
    val state by viewModel.home.collectAsState()
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = state.data.collectionEyebrow,
                    title = state.data.greetingTitle,
                    subtitle = state.data.greetingSubtitle,
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.bell,
                )
            }

            item { PrimaryAction(onClick = onInviteClient) }

            state.data.nowWaiting?.let { live ->
                item { NowSection(live, onOpen = { onOpenIncoming(live) }) }
            }

            item { UpcomingSection(title = "Próximas", sessions = state.data.today, onOpen = onOpenPrep) }

            if (state.data.thisWeek.isNotEmpty()) {
                item { UpcomingSection(title = "Esta semana", sessions = state.data.thisWeek, onOpen = onOpenPrep) }
            }

            if (state.closedToday.isNotEmpty()) {
                item { ClosedTodaySection(state.closedToday) }
            }

            if (state.data.history.isNotEmpty()) {
                item { HistorySection(state.data.history) }
            }
        }
    }
}

@Composable
private fun PrimaryAction(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Btn(
            text = "Iniciar nova sessão",
            onClick = onClick,
            kind = BtnKind.Primary,
            size = BtnSize.Lg,
            icon = TrovataIcons.plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NowSection(live: LiveWaitingSession, onOpen: () -> Unit) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Agora",
            action = { Pill(text = "Aguardando você", tone = PillTone.Live, icon = TrovataIcons.bell) },
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(name = live.client.name, hue = live.client.hue, size = 42.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${live.client.name.split(' ').first()} — ${live.client.shop}",
                            color = colors.ink,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.01).em,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Loja em ${live.client.city} · ${live.client.previousSessions} sessões anteriores",
                            color = colors.ink3,
                            fontSize = 12.sp,
                        )
                    }
                    Btn(text = "Atender", onClick = onOpen, kind = BtnKind.Jade, size = BtnSize.Sm)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.line),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(colors.live, CircleShape),
                    )
                    Text(
                        text = "${live.openedFor} · ${live.viewingHint}",
                        color = colors.ink3,
                        fontSize = 11.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingSection(
    title: String,
    sessions: List<UpcomingSession>,
    onOpen: (UpcomingSession) -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = title)
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                sessions.forEachIndexed { index, session ->
                    UpcomingRow(session = session)
                    if (index < sessions.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.line),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(session: UpcomingSession) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(name = session.client.name, hue = session.client.hue, size = 36.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = session.client.name,
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${session.client.shop} · ${session.itemsHint}",
                color = colors.ink3,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session.tag?.let { tag ->
                val (tone, icon) = tagStyle(tag)
                Pill(
                    text = tag.label,
                    tone = tone,
                    icon = icon,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = session.time,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 14.sp,
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = session.day,
                color = colors.ink4,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun HistorySection(items: List<HistorySession>) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Histórico",
            action = {
                Text(
                    text = "Ver todas",
                    color = colors.brand,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            },
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                items.forEachIndexed { index, entry ->
                    HistoryRow(entry)
                    if (index < items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.line),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistorySession) {
    val colors = TrovataTokens.colors
    val isClosed = entry.status == HistoryStatus.Fechado
    val statusBg = if (isClosed) colors.jadeTint else colors.surface2
    val statusFg = if (isClosed) colors.jade2 else colors.ink3
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(statusBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.check,
                contentDescription = null,
                tint = statusFg,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.client.name,
                    color = colors.ink,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = " · ${entry.client.shop}",
                    color = colors.ink4,
                    fontSize = 13.5.sp,
                )
            }
            Text(
                text = "${entry.items} itens · ${entry.status.label}",
                color = colors.ink3,
                fontSize = 11.5.sp,
            )
        }
        Text(
            text = entry.total,
            style = TrovataTokens.type.mono.copy(
                fontSize = 13.5.sp,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private fun tagStyle(tag: SessionTag): Pair<PillTone, ImageVector> = when (tag) {
    SessionTag.PrimeiraSessao -> PillTone.Brand to TrovataIcons.zap
    SessionTag.Reposicao -> PillTone.Jade to TrovataIcons.trend
    SessionTag.TopVenda -> PillTone.Warn to TrovataIcons.flame
}

@Composable
private fun ClosedTodaySection(orders: List<StoredOrder>) {
    val colors = TrovataTokens.colors
    val totalCents = orders.sumOf { it.totalCents }
    val totalUnits = orders.sumOf { order -> order.lines.sumOf { it.units } }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Pedidos fechados hoje",
            action = {
                Pill(
                    text = "${orders.size} · ${formatBrl(totalCents)}",
                    tone = PillTone.Jade,
                    icon = TrovataIcons.check,
                )
            },
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                orders.forEachIndexed { index, order ->
                    ClosedOrderRow(order)
                    if (index < orders.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.line),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.line),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Total do dia · $totalUnits un",
                        color = colors.ink3,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatBrl(totalCents),
                        style = TrovataTokens.type.mono.copy(
                            fontSize = 14.sp,
                            color = colors.jade2,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClosedOrderRow(order: StoredOrder) {
    val colors = TrovataTokens.colors
    val clientLabel = order.clientName ?: "Cliente"
    val firstName = clientLabel.split(' ').first()
    val unitsTotal = order.lines.sumOf { it.units }
    val skuCount = order.lines.distinctBy { it.productId }.size
    val timeLabel = formatTime(order.createdAtMs)
    val statusBg = if (order.confirmedByMe) colors.jadeTint else colors.surface2
    val statusFg = if (order.confirmedByMe) colors.jade2 else colors.ink3
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(statusBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.check,
                contentDescription = null,
                tint = statusFg,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = firstName,
                    color = colors.ink,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                order.clientShop?.let { shop ->
                    Text(
                        text = " · $shop",
                        color = colors.ink4,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "$unitsTotal un · $skuCount SKUs · $timeLabel",
                color = colors.ink3,
                fontSize = 11.5.sp,
            )
            Text(
                text = order.orderId,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 10.5.sp,
                    color = colors.ink4,
                ),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = formatBrl(order.totalCents),
            style = TrovataTokens.type.mono.copy(
                fontSize = 13.5.sp,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private fun formatBrl(totalCents: Long): String {
    val reais = totalCents / 100
    val cents = (totalCents % 100).let { if (it < 0) -it else it }
    val reaisFormatted = reais.toString().reversed().chunked(3).joinToString(".").reversed()
    val centsFormatted = cents.toString().padStart(2, '0')
    return "R$ $reaisFormatted,$centsFormatted"
}

private fun formatTime(ms: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val hh = dateTime.hour.toString().padStart(2, '0')
    val mm = dateTime.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}
