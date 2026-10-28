package app.trovata.cast.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.trovata.cast.di.CallSession
import app.trovata.cast.feature.aftercall.AfterCallRoute
import app.trovata.cast.platform.LiveCatalogWebView
import app.trovata.cast.platform.PlatformBackHandler
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
            clientName = clientName,
            screenModel = screenModel,
            onHangup = {
                val review = state.hasSomethingToReview
                val carrinhoId = state.carrinhoId
                screenModel.hangup()
                if (review) navigator.replace(AfterCallRoute(sessionId, carrinhoId)) else navigator.pop()
            },
        )
    }
}

@Composable
private fun LiveCallBody(
    state: LiveCallUiState,
    clientName: String?,
    screenModel: LiveCallScreenModel,
    onHangup: () -> Unit,
) {
    val colors = TrovataTokens.colors
    var confirmHangup by rememberSaveable { mutableStateOf(false) }
    PlatformBackHandler {
        when {
            state.summary != null -> onHangup()
            state.showCartDrawer -> screenModel.dismissCartDrawer()
            state.drawing -> screenModel.toggleDrawing()
            else -> confirmHangup = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CallTopBar(state = state, clientName = clientName)
            LiveCatalogWebView(
                url = state.pageUrl,
                bridge = screenModel.webBridge,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            state.toast?.let { CartToastView(toast = it) }
            state.cartError?.let { message ->
                CartErrorBanner(
                    message = message,
                    canRetry = state.cartStage == CartStage.Failed,
                    onRetry = screenModel::retryCart,
                    onDismiss = screenModel::clearCartError,
                )
            }
            CallActionBar(
                state = state,
                onHangup = { confirmHangup = true },
                onToggleMute = screenModel::toggleMute,
                onToggleDrawing = screenModel::toggleDrawing,
                onClearDrawing = screenModel::clearDrawing,
                onOpenCart = screenModel::openCartDrawer,
            )
        }

        if (state.showCartDrawer) {
            CartDrawer(
                state = state,
                onDismiss = screenModel::dismissCartDrawer,
                onConfirmOrder = screenModel::confirmOrder,
            )
        }
        state.summary?.let { summary ->
            OrderSummaryOverlay(summary = summary, onClose = onHangup, modifier = Modifier.fillMaxSize())
        }
    }

    if (confirmHangup) {
        AlertDialog(
            onDismissRequest = { confirmHangup = false },
            title = { Text("Encerrar atendimento?") },
            text = { Text("A chamada será encerrada. O carrinho continua salvo no Catálogo Link.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmHangup = false
                    onHangup()
                }) { Text("Encerrar", color = colors.live) }
            },
            dismissButton = {
                TextButton(onClick = { confirmHangup = false }) { Text("Continuar atendendo") }
            },
            containerColor = colors.surface,
        )
    }
}
