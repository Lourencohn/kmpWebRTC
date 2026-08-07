package app.trovata.cast.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
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
import app.trovata.cast.ui.components.PlaceholderBar
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun SellerHomeScreen(
    viewModel: SessionsViewModel,
    modifier: Modifier = Modifier,
    onInviteClient: () -> Unit = {},
    onOpenSession: (StoredSessionRecord) -> Unit = {},
    onOpenAccount: () -> Unit = {},
) {
    val state by viewModel.home.collectAsState()
    val colors = TrovataTokens.colors
    val isEmpty = state.recentSessions.isEmpty() && state.closedToday.isEmpty()

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = state.eyebrow,
                    title = state.title,
                    subtitle = state.subtitle,
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.bell,
                )
            }

            item { PrimaryAction(onClick = onInviteClient) }

            if (state.closedToday.isNotEmpty()) {
                item { ClosedTodaySection(state.closedToday) }
            }

            if (state.recentSessions.isNotEmpty()) {
                item { RecentSessionsSection(sessions = state.recentSessions, onOpen = onOpenSession) }
            }

            if (isEmpty) {
                item { ClosedTodayScaffold() }
                item { RecentSessionsScaffold() }
                item { EmptyHint() }
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
private fun RecentSessionsSection(
    sessions: List<StoredSessionRecord>,
    onOpen: (StoredSessionRecord) -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Sessões recentes")
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                sessions.forEachIndexed { index, session ->
                    RecentSessionRow(session = session, onOpen = { onOpen(session) })
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
private fun RecentSessionRow(session: StoredSessionRecord, onOpen: () -> Unit) {
    val colors = TrovataTokens.colors
    val clientLabel = session.clientName ?: session.clientShop ?: "Sessão sem cliente"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = clientLabel, hue = hueFor(clientLabel), size = 36.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = clientLabel,
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(session.clientShop.takeIf { session.clientName != null }, session.catalogLabel)
                    .joinToString(" · ")
                    .ifBlank { session.catalogLabel },
                color = colors.ink3,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = formatTime(session.createdAtMs),
                style = TrovataTokens.type.mono.copy(
                    fontSize = 13.sp,
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "Token ${session.token}",
                color = colors.ink4,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun ClosedTodayScaffold() {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Pedidos fechados hoje")
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                repeat(2) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(36.dp).background(colors.surface2, RoundedCornerShape(8.dp)))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlaceholderBar(modifier = Modifier.fillMaxWidth(0.5f))
                            PlaceholderBar(modifier = Modifier.fillMaxWidth(0.7f), height = 9.dp)
                        }
                        PlaceholderBar(modifier = Modifier.width(56.dp), height = 11.dp)
                    }
                    if (index < 1) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSessionsScaffold() {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Sessões recentes")
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                repeat(3) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(36.dp).background(colors.surface2, CircleShape))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlaceholderBar(modifier = Modifier.fillMaxWidth(0.45f))
                            PlaceholderBar(modifier = Modifier.fillMaxWidth(0.65f), height = 9.dp)
                        }
                        PlaceholderBar(modifier = Modifier.width(40.dp), height = 11.dp)
                    }
                    if (index < 2) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    val colors = TrovataTokens.colors
    Text(
        text = "Inicie uma sessão para mostrar o catálogo ao cliente em tempo real. Ela aparece aqui depois.",
        color = colors.ink4,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
    )
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

private fun hueFor(seed: String): Double {
    val hash = seed.fold(0) { acc, c -> acc * 31 + c.code }
    return (((hash % 360) + 360) % 360).toDouble()
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
