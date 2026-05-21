package app.trovata.cast.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.SessionChecklistItem
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
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun SessionPrepScreen(
    viewModel: SessionsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onStartCall: () -> Unit = {},
) {
    val state by viewModel.prep.collectAsState()
    val colors = TrovataTokens.colors
    val data = state.data
    val readyCount = data.checklist.count { it.ready }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBtn(icon = TrovataIcons.back, onClick = onBack, kind = IconBtnKind.Line)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Preparando sessão",
                            color = colors.ink4,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${data.client.name.split(' ').first()} · ${data.client.shop}",
                            color = colors.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box(modifier = Modifier.size(38.dp))
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    ClientSummaryCard(
                        name = data.client.name,
                        shop = data.client.shop,
                        city = data.client.city,
                        hue = data.client.hue,
                        scheduledFor = data.scheduledFor,
                        items = data.itemsCount,
                        skus = data.skuCount,
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Antes de chamar",
                        action = {
                            Pill(
                                text = "$readyCount de ${data.checklist.size} ok",
                                tone = if (state.ready) PillTone.Jade else PillTone.Neutral,
                                icon = if (state.ready) TrovataIcons.check else null,
                            )
                        },
                    )
                    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                        Column {
                            data.checklist.forEachIndexed { index, item ->
                                ChecklistRow(item = item, onToggle = { viewModel.toggleChecklistItem(item.id) })
                                if (index < data.checklist.lastIndex) {
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

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Sugestões TrovataCast")
                    TrovataCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Pill(text = "3 sugestões", tone = PillTone.Brand, icon = TrovataIcons.sparkle)
                            Text(
                                text = "Diego costuma comprar tricot e vestidos no início da estação. Abra com Outono e priorize reposições.",
                                color = colors.ink2,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            StickyStartBar(
                itemsLabel = "${data.itemsCount} produtos · ${data.skuCount} SKUs",
                ready = state.ready,
                onStartCall = onStartCall,
            )
        }
    }
}

@Composable
private fun ClientSummaryCard(
    name: String,
    shop: String,
    city: String,
    hue: Double,
    scheduledFor: String,
    items: Int,
    skus: Int,
) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatar(name = name, hue = hue, size = 52.dp)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = name,
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                )
                Text(
                    text = "$shop · $city",
                    color = colors.ink3,
                    fontSize = 12.5.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(text = scheduledFor, tone = PillTone.Brand, icon = TrovataIcons.clock)
                    Pill(text = "$items itens · $skus SKUs", tone = PillTone.Neutral, icon = TrovataIcons.layers)
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(item: SessionChecklistItem, onToggle: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ChecklistMark(ready = item.ready)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.detail,
                color = colors.ink3,
                fontSize = 12.5.sp,
            )
        }
        ChecklistTrailingIcon(ready = item.ready)
    }
}

@Composable
private fun ChecklistMark(ready: Boolean) {
    val colors = TrovataTokens.colors
    val bg = if (ready) colors.jade else colors.surface2
    val fg = if (ready) Color.White else colors.ink4
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(bg, CircleShape)
            .border(1.dp, if (ready) Color.Transparent else colors.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (ready) {
            Icon(
                imageVector = TrovataIcons.check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ChecklistTrailingIcon(ready: Boolean) {
    val colors = TrovataTokens.colors
    Text(
        text = if (ready) "pronto" else "tocar",
        color = if (ready) colors.jade2 else colors.brand,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun StickyStartBar(itemsLabel: String, ready: Boolean, onStartCall: () -> Unit) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemsLabel,
                    color = colors.ink4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (ready) "Tudo pronto para chamar" else "Falta um item para chamar",
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Btn(
                text = "Chamar agora",
                onClick = onStartCall,
                kind = if (ready) BtnKind.Jade else BtnKind.Soft,
                size = BtnSize.Md,
                icon = TrovataIcons.video,
                enabled = ready,
            )
        }
    }
}
