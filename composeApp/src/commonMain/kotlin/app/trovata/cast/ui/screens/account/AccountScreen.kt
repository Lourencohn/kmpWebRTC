package app.trovata.cast.ui.screens.account

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
import app.trovata.cast.data.sample.AccountRow
import app.trovata.cast.data.sample.AccountStat
import app.trovata.cast.data.sample.SampleAccount
import app.trovata.cast.data.sample.SupportRow
import app.trovata.cast.feature.account.AccountUiState
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun AccountScreen(
    state: AccountUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { TopBar(onBack = onBack) }

            item { ProfileHeader(state) }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Empresa representada",
                        action = { Pill(text = "Sincronizada", tone = PillTone.Jade, icon = TrovataIcons.check) },
                    )
                    BrandIdentityCard(state)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Performance este mês",
                        action = {
                            Text(text = "maio · em curso", color = colors.ink4, fontSize = 11.sp)
                        },
                    )
                    PerformanceStrip(state.performance)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Conta")
                    AccountRowsCard(state.accountRows)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Suporte e privacidade")
                    SupportRowsCard(state.support)
                }
            }

            item { SignOutButton(onClick = onSignOut) }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.back,
                contentDescription = "Voltar",
                tint = colors.ink,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(text = "Minha conta", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            imageVector = TrovataIcons.share,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ProfileHeader(state: AccountUiState) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Avatar(name = state.displayName, hue = SampleAccount.avatarHue, size = 86.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .shadow(1.dp, CircleShape, clip = false)
                    .background(colors.surface, CircleShape)
                    .border(1.dp, colors.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TrovataIcons.sliders,
                    contentDescription = null,
                    tint = colors.ink2,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Text(
            text = state.displayName,
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.025).em,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = state.role,
            color = colors.ink3,
            fontSize = 13.sp,
        )
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
                .background(colors.surface, RoundedCornerShape(999.dp))
                .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountInline(value = state.activeClients.toString(), label = " clientes")
            Text(text = "·", color = colors.ink5, fontSize = 11.sp)
            CountInline(value = state.monthsOnNetwork.toString(), label = " meses na rede")
            Text(text = "·", color = colors.ink5, fontSize = 11.sp)
            Text(
                text = "★ ${state.tierLabel}",
                color = colors.jade2,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CountInline(value: String, label: String) {
    val colors = TrovataTokens.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            color = colors.ink,
            style = TrovataTokens.type.mono.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = label,
            color = colors.ink3,
            fontSize = 11.5.sp,
        )
    }
}

@Composable
private fun BrandIdentityCard(state: AccountUiState) {
    val colors = TrovataTokens.colors
    val brand = SampleAccount.brand
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape, clip = false)
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(SampleAccount.brandGradientTop, SampleAccount.brandGradientBottom),
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "EMPRESA",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.16.em,
                    )
                    Text(
                        text = state.brandName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.03).em,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    brand.colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(c, CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        )
                    }
                }
            }
            Text(
                text = state.brandSubtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Btn(
                text = "Catálogos",
                onClick = {},
                kind = BtnKind.Surface,
                size = BtnSize.Sm,
                icon = TrovataIcons.grid,
                modifier = Modifier.weight(1f),
            )
            Btn(
                text = "Tabelas",
                onClick = {},
                kind = BtnKind.Surface,
                size = BtnSize.Sm,
                icon = TrovataIcons.sliders,
                modifier = Modifier.weight(1f),
            )
            Btn(
                text = "Marca",
                onClick = {},
                kind = BtnKind.Ghost,
                size = BtnSize.Sm,
                icon = TrovataIcons.plus,
            )
        }
    }
}

@Composable
private fun PerformanceStrip(stats: List<AccountStat>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stats.forEach { stat ->
            PerformanceCard(stat = stat, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PerformanceCard(stat: AccountStat, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = modifier, padding = 10.dp) {
        Column {
            Text(
                text = stat.label.uppercase(),
                color = colors.ink4,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = stat.value,
                color = colors.ink,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = stat.delta,
                color = if (stat.deltaPositive) colors.jade2 else colors.ink3,
                style = TrovataTokens.type.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
private fun AccountRowsCard(rows: List<AccountRow>) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
        Column {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(colors.surface2, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = row.icon,
                            contentDescription = null,
                            tint = colors.ink2,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.label,
                            color = colors.ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = row.value,
                            color = colors.ink3,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    row.pill?.let { pill ->
                        Pill(
                            text = pill,
                            tone = if (row.pillJade) PillTone.Jade else PillTone.Neutral,
                            icon = if (row.pillJade) TrovataIcons.check else null,
                        )
                    }
                    Icon(
                        imageVector = TrovataIcons.chev,
                        contentDescription = null,
                        tint = colors.ink4,
                        modifier = Modifier.size(15.dp),
                    )
                }
                if (index < rows.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun SupportRowsCard(rows: List<SupportRow>) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
        Column {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = row.icon,
                        contentDescription = null,
                        tint = colors.ink3,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = row.label,
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (row.value != null) {
                        Text(
                            text = row.value,
                            color = colors.ink4,
                            style = TrovataTokens.type.mono.copy(fontSize = 11.5.sp),
                        )
                    } else {
                        Icon(
                            imageVector = TrovataIcons.chev,
                            contentDescription = null,
                            tint = colors.ink4,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                if (index < rows.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Sair desta conta",
            color = colors.live,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.01).em,
        )
    }
}
