package app.trovata.cast.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.di.CallSession
import app.trovata.cast.platform.LiveCatalogWebView
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

data class LiveCallRoute(
    val token: String,
    val sessionId: String,
    val inviteUrl: String,
    val empresaSlug: String,
    val catalogoUuid: String,
    val sellerName: String,
    val clientName: String?,
    val clientEmail: String? = null,
    val catalogoLinkId: Long? = null,
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
