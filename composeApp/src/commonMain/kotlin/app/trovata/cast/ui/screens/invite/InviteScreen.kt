package app.trovata.cast.ui.screens.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.platform.ShareController
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.QrCard
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.screens.call.LiveCallScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

data class InviteScreen(val record: StoredSessionRecord) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val share = koinInject<ShareController>()
        InviteBody(
            record = record,
            share = share,
            onBack = { navigator.pop() },
            onClose = { navigator.popUntilRoot() },
            onStartCall = {
                navigator.push(
                    LiveCallScreen(
                        token = record.token,
                        sessionId = record.sessionId,
                        empresaSlug = record.empresaSlug,
                        catalogoUuid = record.catalogoUuid,
                        sellerName = record.sellerName,
                        clientName = record.clientName,
                        collectionLabel = record.catalogLabel,
                    ),
                )
            },
        )
    }
}

@Composable
private fun InviteBody(
    record: StoredSessionRecord,
    share: ShareController,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onStartCall: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val shareSubject = "Catálogo TrovataCast"
    val sellerLine = record.sellerName
    val clientFirstName = record.clientName?.split(' ')?.firstOrNull()
    val greeting = if (clientFirstName != null) "Oi, $clientFirstName!" else "Olá!"
    val shareText = buildString {
        append(greeting)
        append(' ')
        append("Acabei de preparar uma sessão pra você: ")
        append(record.catalogLabel)
        append(". Abre o link no celular, sem precisar instalar nada: ")
        append(record.url)
    }
    val whatsappText = shareText

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
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
                            text = "Convite gerado",
                            color = colors.ink4,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = clientFirstName?.let { "Para $it" } ?: "Pronto para enviar",
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
                    SuccessHero(record = record, sellerLine = sellerLine)
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "QR para o cliente",
                        action = {
                            Pill(
                                text = "Aponta a câmera",
                                tone = PillTone.Brand,
                                icon = TrovataIcons.eye,
                            )
                        },
                    )
                    TrovataCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            QrCard(data = record.url, size = 132.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mostre na tela",
                                    color = colors.ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Funciona quando estão lado a lado. O cliente abre a câmera do celular e cai direto na sessão.",
                                    color = colors.ink3,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Link da sessão",
                        action = {
                            Pill(
                                text = "Válido 24h",
                                tone = PillTone.Neutral,
                                icon = TrovataIcons.clock,
                            )
                        },
                    )
                    LinkCard(
                        url = record.url,
                        copied = copied,
                        onCopy = {
                            clipboard.setText(AnnotatedString(record.url))
                            copied = true
                        },
                        onShareLinkOnly = { share.share(record.url, shareSubject) },
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Compartilhar")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Btn(
                            text = "WhatsApp",
                            onClick = { share.share(whatsappText, shareSubject) },
                            kind = BtnKind.Jade,
                            size = BtnSize.Md,
                            icon = TrovataIcons.send,
                            modifier = Modifier.weight(1f),
                        )
                        Btn(
                            text = "Outro app",
                            onClick = { share.share(shareText, shareSubject) },
                            kind = BtnKind.Soft,
                            size = BtnSize.Md,
                            icon = TrovataIcons.share,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Próximos passos")
                    TrovataCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NextStepRow(
                                index = 1,
                                title = "Cliente abre o link",
                                detail = "Sem instalar nada. Web abre direto.",
                            )
                            NextStepRow(
                                index = 2,
                                title = "Vocês entram juntos",
                                detail = "Áudio + catálogo sincronizado.",
                            )
                            NextStepRow(
                                index = 3,
                                title = "Pedido nasce na call",
                                detail = "Sem digitação depois.",
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
            StickyClose(onClose = onClose, onStartCall = onStartCall)
        }
    }
}

@Composable
private fun SuccessHero(record: StoredSessionRecord, sellerLine: String) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.jadeTint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TrovataIcons.check,
                    contentDescription = null,
                    tint = colors.jade2,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sessão pronta",
                    color = colors.ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.01).em,
                )
                Text(
                    text = "$sellerLine · ${record.catalogLabel}",
                    color = colors.ink3,
                    fontSize = 12.5.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(text = "Token ${record.token}", tone = PillTone.Brand, icon = TrovataIcons.link)
                    record.clientShop?.let {
                        Pill(text = it, tone = PillTone.Neutral, icon = TrovataIcons.user)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkCard(
    url: String,
    copied: Boolean,
    onCopy: () -> Unit,
    onShareLinkOnly: () -> Unit,
) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(colors.brandTint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TrovataIcons.link,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = url,
                    style = TrovataTokens.type.mono.copy(
                        fontSize = 13.sp,
                        color = colors.ink,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(
                    text = if (copied) "Copiado" else "Copiar link",
                    onClick = onCopy,
                    kind = if (copied) BtnKind.Jade else BtnKind.Primary,
                    size = BtnSize.Sm,
                    icon = if (copied) TrovataIcons.check else TrovataIcons.copy,
                    modifier = Modifier.weight(1f),
                )
                Btn(
                    text = "Enviar link",
                    onClick = onShareLinkOnly,
                    kind = BtnKind.Ghost,
                    size = BtnSize.Sm,
                    icon = TrovataIcons.share,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NextStepRow(index: Int, title: String, detail: String) {
    val colors = TrovataTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(colors.surface2, CircleShape)
                .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                color = colors.ink2,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.ink,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                color = colors.ink3,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun StickyClose(onClose: () -> Unit, onStartCall: () -> Unit) {
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Btn(
                text = "Concluir",
                onClick = onClose,
                kind = BtnKind.Ghost,
                size = BtnSize.Md,
                icon = TrovataIcons.check,
                modifier = Modifier.weight(1f),
            )
            Btn(
                text = "Iniciar chamada",
                onClick = onStartCall,
                kind = BtnKind.Jade,
                size = BtnSize.Md,
                icon = TrovataIcons.video,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

