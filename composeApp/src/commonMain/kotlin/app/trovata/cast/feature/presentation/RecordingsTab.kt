package app.trovata.cast.feature.presentation

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.platform.ShareController
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.NumberedStep
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

internal const val CopiedFeedbackMs: Long = 2_000

@Composable
fun RecordingsTab(
    screenModel: RecordingsScreenModel,
    accountName: String,
    onOpenAccount: () -> Unit,
    onRecordNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()
    val colors = TrovataTokens.colors
    val share = koinInject<ShareController>()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var copiedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copiedId) {
        if (copiedId != null) {
            delay(CopiedFeedbackMs)
            copiedId = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = state.companyName.ifBlank { "Apresentações gravadas" },
                    title = "Gravações",
                    subtitle = state.subtitle,
                    accountName = accountName,
                    onOpenAccount = onOpenAccount,
                )
            }

            item { NewRecordingCard(onClick = onRecordNew) }

            if (state.isEmpty) {
                item { EmptyRecordings() }
            }

            if (state.available.isNotEmpty()) {
                item {
                    SectionLabel(
                        text = "Links para enviar",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        action = { Pill(text = "${state.available.size}", tone = PillTone.Jade) },
                    )
                }
                items(state.available, key = { it.presentationId }) { item ->
                    RecordingCard(
                        item = item,
                        copied = copiedId == item.presentationId,
                        onCopy = {
                            clipboard.setText(AnnotatedString(item.shareUrl))
                            copiedId = item.presentationId
                        },
                        onShare = { share.share(item.shareMessage, "Apresentação ${item.catalogoNome}") },
                        onWatch = { uriHandler.openUri(item.shareUrl) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (state.expired.isNotEmpty()) {
                item {
                    ExpiredSection(
                        items = state.expired,
                        onRemove = screenModel::removeFromList,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewRecordingCard(onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.ink, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(22.dp).background(colors.live, CircleShape))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Gravar nova apresentação",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Fale enquanto mostra a vitrine. O cliente recebe um link e assiste quando puder, sem instalar nada.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
        }
        Btn(
            text = "Escolher catálogo e gravar",
            onClick = onClick,
            kind = BtnKind.Danger,
            size = BtnSize.Md,
            icon = TrovataIcons.mic,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EmptyRecordings() {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(text = "Suas gravações")
        TrovataCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Nenhuma gravação por aqui ainda",
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                NumberedStep(
                    number = 1,
                    title = "Escolha o catálogo",
                    detail = "Toque no botão vermelho acima e selecione o catálogo que vai mostrar.",
                )
                NumberedStep(
                    number = 2,
                    title = "Apresente falando",
                    detail = "Sua voz e cada toque na vitrine entram na gravação. Não é vídeo: o cliente vê a vitrine de verdade.",
                )
                NumberedStep(
                    number = 3,
                    title = "Mande o link",
                    detail = "O mesmo link serve para vários clientes e fica guardado aqui por $PresentationRetentionDays dias.",
                )
            }
        }
    }
}

@Composable
private fun RecordingCard(
    item: RecordingItem,
    copied: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                PlayTile(duration = item.durationLabel, onClick = onWatch)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.catalogoNome,
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(text = item.recordedLabel, color = colors.ink3, fontSize = 12.sp)
                    Text(
                        text = "${item.actionsLabel} na vitrine",
                        color = colors.ink4,
                        fontSize = 11.5.sp,
                    )
                }
            }
            Pill(
                text = item.availabilityLabel,
                tone = if (item.availability == RecordingAvailability.ExpiringSoon) PillTone.Warn else PillTone.Jade,
                icon = TrovataIcons.clock,
            )
            LinkField(url = item.shareUrl, copied = copied, onCopy = onCopy)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(
                    text = "Assistir",
                    onClick = onWatch,
                    kind = BtnKind.Surface,
                    size = BtnSize.Md,
                    icon = TrovataIcons.play,
                )
                Btn(
                    text = "Compartilhar",
                    onClick = onShare,
                    kind = BtnKind.Jade,
                    size = BtnSize.Md,
                    icon = TrovataIcons.share,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlayTile(duration: String, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .size(58.dp)
            .clip(shape)
            .background(colors.ink, shape)
            .clickable(onClickLabel = "Assistir apresentação", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = TrovataIcons.play,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = duration,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun LinkField(url: String, copied: Boolean, onCopy: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface2, shape)
            .border(1.dp, colors.line, shape)
            .clickable(onClickLabel = "Copiar link", onClick = onCopy)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = TrovataIcons.link,
            contentDescription = null,
            tint = colors.ink4,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = url,
            color = colors.ink2,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (copied) TrovataIcons.check else TrovataIcons.copy,
                contentDescription = null,
                tint = if (copied) colors.jade2 else colors.brand,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (copied) "Copiado" else "Copiar",
                color = if (copied) colors.jade2 else colors.brand,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ExpiredSection(items: List<RecordingItem>, onRemove: (String) -> Unit) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Expiradas",
            action = { Pill(text = "${items.size}", tone = PillTone.Neutral) },
        )
        Text(
            text = "O link para de funcionar $PresentationRetentionDays dias depois do envio. Se o cliente ainda precisar, grave de novo.",
            color = colors.ink4,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        )
        TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp, elevated = false) {
            Column {
                items.forEachIndexed { index, item ->
                    ExpiredRow(item = item, onRemove = { onRemove(item.presentationId) })
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
private fun ExpiredRow(item: RecordingItem, onRemove: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.surface2, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.clock,
                contentDescription = null,
                tint = colors.ink4,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.catalogoNome,
                color = colors.ink3,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.availabilityLabel} · ${item.durationLabel}",
                color = colors.ink4,
                fontSize = 11.5.sp,
            )
        }
        Btn(
            text = "Remover",
            onClick = onRemove,
            kind = BtnKind.Ghost,
            size = BtnSize.Sm,
            icon = TrovataIcons.trash,
        )
    }
}
