package app.trovata.cast.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.feature.call.LiveCallScreenModel
import app.trovata.cast.feature.call.LiveCallUiState
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ProductCard
import app.trovata.cast.ui.components.ProductCardSize
import app.trovata.cast.ui.icons.TrovataIcons
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class LiveCallScreen(val token: String, val sellerName: String) : Screen {

    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            LiveCallScreenModel(
                token = token,
                httpClient = container.httpClient,
                sellerName = sellerName,
            )
        }
        LaunchedEffect(token) { screenModel.start() }

        val state by screenModel.state.collectAsState()
        LiveCallBody(
            state = state,
            onHangup = {
                screenModel.hangup()
                navigator.pop()
            },
            onToggleMute = { screenModel.toggleMute() },
            onScroll = { ref, offset -> screenModel.publishScroll(ref, offset) },
            onPointAt = { ref -> screenModel.publishPointAt(ref) },
        )
    }
}

@Composable
private fun LiveCallBody(
    state: LiveCallUiState,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onScroll: (productId: String, offset: Float) -> Unit,
    onPointAt: (productId: String) -> Unit,
) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CallTopBar(state = state, modifier = Modifier.fillMaxWidth())

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.isLive) {
                    CatalogPanel(
                        onScroll = onScroll,
                        onPointAt = onPointAt,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    IdleHero(state = state, modifier = Modifier.fillMaxSize())
                }
            }

            CallActionBar(
                state = state,
                onHangup = onHangup,
                onToggleMute = onToggleMute,
                modifier = Modifier.fillMaxWidth(),
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
private fun CatalogPanel(
    onScroll: (productId: String, offset: Float) -> Unit,
    onPointAt: (productId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val gridState = rememberLazyGridState()
    var pointing by remember { mutableStateOf(false) }
    var pointedRef by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pointedRef) {
        if (pointedRef == null) return@LaunchedEffect
        kotlinx.coroutines.delay(3_000)
        pointedRef = null
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offsetPx) ->
            val product = SampleCatalog.products.getOrNull(index) ?: return@collect
            val normalized = (offsetPx.toFloat() / 320f).coerceIn(0f, 1f)
            onScroll(product.ref, normalized)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SampleCatalog.collection.uppercase(),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.5.sp,
                    letterSpacing = 0.08.em,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (pointing) "Toque num produto para apontar" else "Mostrando para o cliente",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconBtn(
                icon = TrovataIcons.pointer,
                onClick = { pointing = !pointing },
                kind = if (pointing) IconBtnKind.Brand else IconBtnKind.Dark,
                active = pointing,
                size = 40.dp,
                contentDescription = if (pointing) "Sair do modo apontar" else "Entrar no modo apontar",
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.ink),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(SampleCatalog.products, key = { it.ref }) { product ->
                ProductCard(
                    product = product,
                    size = ProductCardSize.Md,
                    pointed = pointedRef == product.ref,
                    onClick = if (pointing) {
                        {
                            pointedRef = product.ref
                            onPointAt(product.ref)
                        }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun IdleHero(state: LiveCallUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CallAvatar(state = state)
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = headlineFor(state),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = state.errorMessage ?: subheadlineFor(state),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CallActionBar(
    state: LiveCallUiState,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
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

private fun headlineFor(state: LiveCallUiState): String = when {
    state.errorMessage != null -> "Falha na chamada"
    state.isLive -> "Em chamada"
    state.isNegotiating -> "Cliente entrando…"
    else -> "Aguardando cliente entrar"
}

private fun subheadlineFor(state: LiveCallUiState): String = when {
    state.isLive && state.remoteMuted -> "Cliente sem áudio · token ${state.token}"
    state.isLive -> "Áudio P2P ativo · token ${state.token}"
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
private fun CallAvatar(state: LiveCallUiState) {
    val colors = TrovataTokens.colors
    val borderColor = when {
        state.errorMessage != null -> colors.live
        state.isLive -> colors.jade
        else -> Color.White.copy(alpha = 0.25f)
    }
    Box(
        modifier = Modifier
            .size(140.dp)
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
            .border(3.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "C",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
