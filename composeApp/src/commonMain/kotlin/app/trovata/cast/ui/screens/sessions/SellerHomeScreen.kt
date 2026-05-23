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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.HistorySession
import app.trovata.cast.data.sample.HistoryStatus
import app.trovata.cast.data.sample.LiveWaitingSession
import app.trovata.cast.data.sample.SessionTag
import app.trovata.cast.data.sample.UpcomingSession
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ScreenHeader
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.components.Wordmark
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun SellerHomeScreen(
    viewModel: SessionsViewModel,
    modifier: Modifier = Modifier,
    onOpenIncoming: (LiveWaitingSession) -> Unit = {},
    onOpenPrep: (UpcomingSession) -> Unit = {},
    onInviteClient: () -> Unit = {},
) {
    val state by viewModel.home.collectAsState()
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { HomeHeader(state.data.collectionEyebrow, state.data.greetingTitle, state.data.greetingSubtitle) }

            item { PrimaryAction(onClick = onInviteClient) }

            state.data.nowWaiting?.let { live ->
                item { NowSection(live, onOpen = { onOpenIncoming(live) }) }
            }

            item { UpcomingSection(title = "Próximas", sessions = state.data.today, onOpen = onOpenPrep) }

            if (state.data.thisWeek.isNotEmpty()) {
                item { UpcomingSection(title = "Esta semana", sessions = state.data.thisWeek, onOpen = onOpenPrep) }
            }

            if (state.data.history.isNotEmpty()) {
                item { HistorySection(state.data.history) }
            }
        }

        InviteFab(
            onClick = onInviteClient,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun HomeHeader(eyebrow: String, title: String, subtitle: String) {
    val colors = TrovataTokens.colors
    ScreenHeader(
        title = title,
        subtitle = subtitle,
        eyebrow = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(height = 22.dp)
                Text(
                    text = eyebrow.uppercase(),
                    color = colors.ink4,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.08.em,
                )
            }
        },
        trailing = { IconBtn(icon = TrovataIcons.bell, onClick = {}, kind = IconBtnKind.Line) },
    )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(name = session.client.name, hue = session.client.hue, size = 36.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.client.name,
                                color = colors.ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${session.client.shop} · ${session.itemsHint}",
                                    color = colors.ink3,
                                    fontSize = 11.5.sp,
                                )
                                session.tag?.let { tag ->
                                    Pill(
                                        text = tag.label,
                                        tone = if (tag == SessionTag.PrimeiraSessao) PillTone.Brand else PillTone.Neutral,
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
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

@Composable
private fun InviteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .background(colors.ink, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = TrovataIcons.plus,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Convidar cliente",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
