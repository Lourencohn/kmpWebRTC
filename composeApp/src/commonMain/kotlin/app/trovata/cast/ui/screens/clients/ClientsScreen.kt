package app.trovata.cast.ui.screens.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.Client
import app.trovata.cast.data.sample.ClientTag
import app.trovata.cast.data.sample.ReadyToApproach
import app.trovata.cast.data.sample.ReadyUrgency
import app.trovata.cast.data.sample.SampleClients
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.Sparkline
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun ClientsScreen(
    modifier: Modifier = Modifier,
    onOpenAccount: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = "Carteira · 47 ativos",
                    title = "Clientes",
                    subtitle = "8 prontos para abordar · 3 ao vivo agora",
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.filter,
                )
            }

            item { SmartSuggestion(SampleClients.readyToApproach) }

            item { SearchBox() }

            item { SegmentChips() }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Esta semana",
                        action = {
                            Pill(text = "3 ao vivo", tone = PillTone.Live, icon = TrovataIcons.signal)
                        },
                    )
                    ClientList(SampleClients.recent)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Este mês")
                    ClientList(SampleClients.month)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        ) {
            Btn(
                text = "Cliente",
                onClick = {},
                kind = BtnKind.Dark,
                size = BtnSize.Md,
                icon = TrovataIcons.plus,
                modifier = Modifier.shadow(elevation = 12.dp, shape = RoundedCornerShape(999.dp), clip = false),
            )
        }
    }
}

@Composable
private fun SmartSuggestion(items: List<ReadyToApproach>) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(1.dp, shape, clip = false)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(colors.brandTint, colors.surface),
                ),
                shape = shape,
            )
            .border(1.dp, colors.brandTint, shape),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.brand, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TrovataIcons.sparkle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SUGESTÕES TC · AGORA",
                    color = colors.brand2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.06.em,
                )
                Text(
                    text = "5 clientes prontos para abordar",
                    color = colors.ink,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Icon(
                imageVector = TrovataIcons.chev,
                contentDescription = null,
                tint = colors.brand2,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(modifier = Modifier.background(colors.surface, shape)) {
            Column {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    }
                    ReadyRow(item)
                }
            }
        }
    }
}

@Composable
private fun ReadyRow(item: ReadyToApproach) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = item.name, hue = item.hue, size = 34.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    color = colors.ink,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = " · ${item.sinceLabel}",
                    color = colors.ink4,
                    fontSize = 12.5.sp,
                )
            }
            Text(
                text = item.shop,
                color = colors.ink3,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Btn(
            text = "Convidar",
            onClick = {},
            kind = if (item.urgency == ReadyUrgency.Urgente) BtnKind.Primary else BtnKind.Soft,
            size = BtnSize.Sm,
            icon = TrovataIcons.video,
        )
    }
}

@Composable
private fun SearchBox() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.surface2, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.search,
            contentDescription = null,
            tint = colors.ink3,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = "Buscar cliente, cidade, loja...",
            color = colors.ink4,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SegmentChips() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SampleClients.segments.forEachIndexed { index, seg ->
            val active = index == 0
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (active) colors.ink else colors.surface,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) colors.ink else colors.line,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = seg.label,
                    color = if (active) Color.White else colors.ink2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                )
                Text(
                    text = seg.count.toString(),
                    style = TrovataTokens.type.mono.copy(
                        fontSize = 10.5.sp,
                        color = if (active) Color.White.copy(alpha = 0.7f) else colors.ink4,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ClientList(clients: List<Client>) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
        Column {
            clients.forEachIndexed { index, client ->
                ClientRow(client)
                if (index < clients.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: Client) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(name = client.name, hue = client.hue, size = 40.dp)
            if (client.live) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .background(colors.jade, CircleShape)
                        .border(2.dp, colors.surface, CircleShape),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = client.name,
                    color = colors.ink,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                client.tag?.let { tag ->
                    Pill(
                        text = when (tag) {
                            ClientTag.Novo -> "Novo"
                            ClientTag.TopVenda -> "Top"
                        },
                        tone = when (tag) {
                            ClientTag.Novo -> PillTone.Brand
                            ClientTag.TopVenda -> PillTone.Jade
                        },
                    )
                }
            }
            Text(
                text = "${client.shop} · ${client.city}",
                color = colors.ink3,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sparkline(
                    data = client.activity,
                    width = 56.dp,
                    height = 16.dp,
                    color = if (client.live) colors.jade else colors.ink4,
                    fill = false,
                    dot = false,
                )
                Text(
                    text = "· ${client.lastSession}",
                    color = colors.ink4,
                    style = TrovataTokens.type.mono.copy(fontSize = 10.5.sp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = client.ltv,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 12.5.sp,
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                ),
            )
            Text(
                text = "LTV",
                color = colors.ink4,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
        }
    }
}
