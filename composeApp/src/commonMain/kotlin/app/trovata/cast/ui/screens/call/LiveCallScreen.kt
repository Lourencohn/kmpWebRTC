package app.trovata.cast.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.di.CallSession
import app.trovata.cast.feature.call.CallSpec
import app.trovata.cast.feature.call.CartLineUi
import app.trovata.cast.feature.call.CartStage
import app.trovata.cast.feature.call.CartToast
import app.trovata.cast.feature.call.LiveCallScreenModel
import app.trovata.cast.feature.call.LiveCallUiState
import app.trovata.cast.feature.call.OrderSummaryUi
import app.trovata.cast.feature.call.newSellerPeerId
import app.trovata.cast.platform.LiveCatalogWebView
import app.trovata.cast.protocol.OrderLine
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.icons.TrovataIcons
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

data class LiveCallScreen(
    val token: String,
    val sessionId: String,
    val inviteUrl: String,
    val empresaSlug: String,
    val catalogoUuid: String,
    val sellerName: String,
    val clientName: String?,
    val clientEmail: String? = null,
    val catalogoLinkId: Long? = null,
    val collectionLabel: String = "",
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val koin = getKoin()
        val sellerPeerId = remember(sessionId) { newSellerPeerId() }
        val screenModel = rememberScreenModel {
            val spec = CallSpec(
                token = token,
                sessionId = sessionId,
                inviteUrl = inviteUrl,
                empresaSlug = empresaSlug,
                catalogoUuid = catalogoUuid,
                clientName = clientName,
                clientEmail = clientEmail,
                catalogoLinkId = catalogoLinkId,
                sellerName = sellerName,
                collectionLabel = collectionLabel,
                sellerPeerId = sellerPeerId,
            )
            val callScope = koin.createScope<CallSession>(sellerPeerId)
            callScope.get<LiveCallScreenModel> { parametersOf(spec) }
        }
        LaunchedEffect(token) { screenModel.start() }

        val state by screenModel.state.collectAsState()

        LaunchedEffect(state.toast?.createdAtMs) {
            if (state.toast == null) return@LaunchedEffect
            kotlinx.coroutines.delay(3_500)
            screenModel.dismissToast()
        }

        LiveCallBody(
            state = state,
            screenModel = screenModel,
            onHangup = {
                screenModel.hangup()
                navigator.pop()
            },
        )
    }
}

@Composable
private fun LiveCallBody(
    state: LiveCallUiState,
    screenModel: LiveCallScreenModel,
    onHangup: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CallTopBar(state = state, modifier = Modifier.fillMaxWidth())

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White),
            ) {
                LiveCatalogWebView(
                    url = state.pageUrl,
                    bridge = screenModel.webBridge,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            CallActionBar(
                state = state,
                onHangup = onHangup,
                onToggleMute = screenModel::toggleMute,
                onToggleDrawing = screenModel::toggleDrawing,
                onClearDrawing = screenModel::clearDrawing,
                onOpenCart = screenModel::openCartDrawer,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.toast?.let { toast ->
            CartToastView(
                toast = toast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp, start = 16.dp, end = 16.dp),
            )
        }

        if (state.showCartDrawer) {
            CartDrawer(
                state = state,
                onDismiss = screenModel::dismissCartDrawer,
                onConfirmOrder = screenModel::confirmOrder,
            )
        }

        state.cartError?.let { message ->
            CartErrorBanner(
                message = message,
                canRetry = state.cartStage == CartStage.Failed,
                onRetry = screenModel::retryCart,
                onDismiss = screenModel::clearCartError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 116.dp, start = 16.dp, end = 16.dp),
            )
        }

        state.summary?.let { summary ->
            OrderSummaryOverlay(
                summary = summary,
                onClose = onHangup,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CallTopBar(state: LiveCallUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.04f))
            .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusPill(state = state)
            Text(
                text = headlineFor(state),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (state.isLive && state.remoteMuted) {
                Pill(text = "Cliente sem áudio", tone = PillTone.Live, icon = TrovataIcons.micOff)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.errorMessage ?: subheadlineFor(state),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.5.sp,
        )
    }
}

@Composable
private fun CallActionBar(
    state: LiveCallUiState,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDrawing: () -> Unit,
    onClearDrawing: () -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.04f))
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(
            icon = if (state.localMuted) TrovataIcons.micOff else TrovataIcons.mic,
            onClick = onToggleMute,
            kind = IconBtnKind.Dark,
            active = state.localMuted,
            size = 48.dp,
            contentDescription = if (state.localMuted) "Reativar microfone" else "Silenciar microfone",
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconBtn(
            icon = TrovataIcons.pointer,
            onClick = onToggleDrawing,
            kind = if (state.drawing) IconBtnKind.Brand else IconBtnKind.Dark,
            active = state.drawing,
            size = 48.dp,
            contentDescription = if (state.drawing) "Parar de desenhar" else "Desenhar para o cliente",
        )
        if (state.drawing) {
            Spacer(modifier = Modifier.width(10.dp))
            IconBtn(
                icon = TrovataIcons.trash,
                onClick = onClearDrawing,
                kind = IconBtnKind.Dark,
                size = 48.dp,
                contentDescription = "Apagar o desenho",
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        CartButton(count = state.cartCount, onClick = onOpenCart)
        Spacer(modifier = Modifier.width(10.dp))
        Btn(
            text = "Encerrar",
            onClick = onHangup,
            kind = BtnKind.Danger,
            size = BtnSize.Md,
            icon = TrovataIcons.hangup,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CartButton(count: Int, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    Box(modifier = Modifier.size(48.dp)) {
        IconBtn(
            icon = TrovataIcons.cart,
            onClick = onClick,
            kind = if (count > 0) IconBtnKind.Jade else IconBtnKind.Dark,
            size = 48.dp,
            contentDescription = "Abrir carrinho",
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(colors.brand, CircleShape)
                    .border(2.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CartToastView(toast: CartToast, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.jade, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TrovataIcons.cart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = toast.text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CartDrawer(
    state: LiveCallUiState,
    onDismiss: () -> Unit,
    onConfirmOrder: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 560.dp)
                .background(colors.bg, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(enabled = false) { }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.line, RoundedCornerShape(2.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pedido em construção",
                        color = colors.ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = cartSubtitleFor(state),
                        color = colors.ink3,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                IconBtn(
                    icon = TrovataIcons.chev,
                    onClick = onDismiss,
                    kind = IconBtnKind.Line,
                    size = 36.dp,
                    contentDescription = "Fechar carrinho",
                )
            }
            if (state.cart.isEmpty()) {
                EmptyCart(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                ) {
                    items(state.cart, key = { it.itemId }) { line ->
                        CartRow(line = line)
                    }
                }
                CartTotalBar(units = state.cartCount, totalCents = state.cartTotalCents)
                Spacer(modifier = Modifier.height(2.dp))
                Btn(
                    text = if (state.isFinishingCart) {
                        "Enviando..."
                    } else {
                        "Marcar pronto para envio · ${formatBrl(state.cartTotalCents)}"
                    },
                    onClick = onConfirmOrder,
                    kind = BtnKind.Jade,
                    size = BtnSize.Lg,
                    icon = TrovataIcons.check,
                    enabled = !state.isFinishingCart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun cartSubtitleFor(state: LiveCallUiState): String = when (state.cartStage) {
    CartStage.Idle -> "Preparando o carrinho do cliente"
    CartStage.Opening -> "Abrindo o carrinho do cliente"
    CartStage.Failed -> state.cartError ?: "Carrinho indisponível"
    CartStage.Ready -> {
        val cliente = state.cartClientName?.takeIf { it.isNotBlank() }
        if (cliente != null) "Carrinho de $cliente no Catálogo Link" else "Carrinho aberto no Catálogo Link"
    }
}

@Composable
private fun CartErrorBanner(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(14.dp))
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            color = colors.ink2,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        if (canRetry) {
            Btn(
                text = "Tentar de novo",
                onClick = onRetry,
                kind = BtnKind.Surface,
                size = BtnSize.Sm,
            )
        }
        IconBtn(
            icon = TrovataIcons.chevDown,
            onClick = onDismiss,
            kind = IconBtnKind.Line,
            size = 32.dp,
            contentDescription = "Fechar aviso",
        )
    }
}

@Composable
private fun CartRow(line: CartLineUi) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CartThumbnail(imageUrl = line.imageUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.name,
                color = colors.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(line.ref, line.color).joinToString(" · "),
                color = colors.ink3,
                fontSize = 11.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = line.sizesLabel.ifBlank { "${line.units}un" },
                color = colors.ink3,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatBrl(line.totalCents),
                color = colors.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CartThumbnail(imageUrl: String?) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(
                imageVector = TrovataIcons.swatch,
                contentDescription = null,
                tint = colors.ink3,
                modifier = Modifier.size(18.dp),
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CartTotalBar(units: Int, totalCents: Long) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface2, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Total",
                color = colors.ink3,
                fontSize = 11.sp,
                letterSpacing = 0.06.em,
            )
            Text(
                text = "${units}un · ${formatBrl(totalCents)}",
                color = colors.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Pill(text = "Ao vivo", tone = PillTone.Jade, icon = TrovataIcons.signal)
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.surface2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.cart,
                contentDescription = null,
                tint = colors.ink3,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = "Carrinho vazio",
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Quando alguém adicionar uma peça na vitrine, ela aparece aqui em tempo real.",
            color = colors.ink3,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

private fun headlineFor(state: LiveCallUiState): String = when {
    state.errorMessage != null -> "Falha na chamada"
    state.isLive && state.drawing -> "Desenhando para o cliente"
    state.isLive -> "Mostrando para o cliente"
    state.isNegotiating -> "Cliente entrando…"
    else -> "Aguardando cliente entrar"
}

private fun subheadlineFor(state: LiveCallUiState): String = when {
    state.isLive && state.remoteMuted -> "Cliente sem áudio · token ${state.token}"
    state.isLive -> "Vocês estão vendo a mesma vitrine · token ${state.token}"
    state.isNegotiating -> "Conectando o áudio · token ${state.token}"
    else -> "Compartilhe o link · token ${state.token}"
}

@Composable
private fun StatusPill(state: LiveCallUiState) {
    val text = when {
        state.errorMessage != null -> "Falha"
        state.isLive -> "Ao vivo"
        state.isNegotiating -> "Conectando…"
        else -> "Aguardando"
    }
    val tone = when {
        state.errorMessage != null -> PillTone.Live
        state.isLive -> PillTone.Jade
        else -> PillTone.Dark
    }
    Pill(text = text, tone = tone, icon = TrovataIcons.signal)
}

@Composable
private fun OrderSummaryOverlay(
    summary: OrderSummaryUi,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = modifier
            .background(colors.bg)
            .padding(top = 56.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Pill(
                text = if (summary.confirmedByMe) "Você confirmou" else "Cliente confirmou",
                tone = PillTone.Jade,
                icon = TrovataIcons.check,
            )
            Text(
                text = "Pedido pronto",
                color = colors.ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
            )
            Text(
                text = "ID ${summary.orderId} · ${summary.lines.sumOf { it.units }} un · ${summary.lines.size} ${if (summary.lines.size == 1) "linha" else "linhas"}",
                color = colors.ink3,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(summary.lines, key = { it.productId + "/" + it.size }) { line ->
                OrderSummaryRow(line = line)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface2)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total do pedido",
                        color = colors.ink3,
                        fontSize = 11.sp,
                        letterSpacing = 0.06.em,
                    )
                    Text(
                        text = formatBrl(summary.totalCents),
                        color = colors.ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Btn(
                text = "Fechar e encerrar",
                onClick = onClose,
                kind = BtnKind.Jade,
                size = BtnSize.Lg,
                icon = TrovataIcons.hangup,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OrderSummaryRow(line: OrderLine) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${line.productId} · Tam ${line.size} · ${line.units}un",
            color = colors.ink2,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatBrl(line.subtotalCents),
            color = colors.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatBrl(cents: Long): String {
    val whole = cents / 100
    val fractional = (cents % 100).toString().padStart(2, '0')
    val wholeWithDots = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    return "R$ $wholeWithDots,$fractional"
}
