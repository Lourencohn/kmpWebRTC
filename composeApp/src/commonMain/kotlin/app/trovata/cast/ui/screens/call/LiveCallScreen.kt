package app.trovata.cast.ui.screens.call

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.data.sample.Product
import app.trovata.cast.feature.call.CartLineUi
import app.trovata.cast.feature.call.CartToast
import app.trovata.cast.feature.call.LiveCallScreenModel
import app.trovata.cast.feature.call.LiveCallUiState
import app.trovata.cast.feature.call.OrderSummaryUi
import app.trovata.cast.protocol.OrderLine
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
import app.trovata.cast.ui.components.ProductRow
import app.trovata.cast.ui.components.ProductRowSize
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.screens.catalog.ProductDetailScreen
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class LiveCallScreen(
    val token: String,
    val sessionId: String,
    val sellerName: String,
    val clientName: String?,
    val collectionLabel: String = "",
) : Screen {

    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            LiveCallScreenModel(
                token = token,
                sessionId = sessionId,
                clientName = clientName,
                httpClient = container.httpClient,
                cartRepository = container.cartRepository,
                orderRepository = container.orderRepository,
                catalogRepository = container.catalogRepository,
                sessionsRepository = container.sessionsRepository,
                collectionLabel = collectionLabel,
                sellerName = sellerName,
            )
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
            onHangup = {
                screenModel.hangup()
                navigator.pop()
            },
            onToggleMute = { screenModel.toggleMute() },
            onScroll = { ref, offset -> screenModel.publishScroll(ref, offset) },
            onPointAt = { ref -> screenModel.publishPointAt(ref) },
            onOpenDetail = { ref -> screenModel.openProductDetail(ref) },
            onOpenCart = { screenModel.openCartDrawer() },
            onDismissCart = { screenModel.dismissCartDrawer() },
            onDismissProductSheet = { screenModel.dismissProductSheet() },
            onConfirmOrder = { screenModel.confirmOrder() },
        )
    }
}

@Composable
private fun LiveCallBody(
    state: LiveCallUiState,
    clientName: String?,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onScroll: (productId: String, offset: Float) -> Unit,
    onPointAt: (productId: String) -> Unit,
    onOpenDetail: (productId: String) -> Unit,
    onOpenCart: () -> Unit,
    onDismissCart: () -> Unit,
    onDismissProductSheet: () -> Unit,
    onConfirmOrder: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val productByRef = remember(state.products) { state.products.associateBy { it.ref } }
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
                        products = state.products,
                        collectionLabel = state.collectionLabel,
                        onScroll = onScroll,
                        onPointAt = onPointAt,
                        onOpenDetail = onOpenDetail,
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
                onOpenCart = onOpenCart,
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

        if (state.showProductSheet && state.focusedProductId != null) {
            val product = productByRef[state.focusedProductId]
            if (product != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg),
                ) {
                    ProductDetailScreen(
                        product = product,
                        related = state.products.filter { it.ref != product.ref }.take(6),
                        inCallContext = true,
                        customerName = clientName,
                        onBack = onDismissProductSheet,
                        onPointAt = onPointAt,
                    )
                }
            }
        }

        if (state.showCartDrawer) {
            CartDrawer(
                state = state,
                productByRef = productByRef,
                onDismiss = onDismissCart,
                onConfirmOrder = onConfirmOrder,
            )
        }

        state.summary?.let { summary ->
            OrderSummaryOverlay(
                summary = summary,
                productByRef = productByRef,
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
private fun CatalogPanel(
    products: List<Product>,
    collectionLabel: String,
    onScroll: (productId: String, offset: Float) -> Unit,
    onPointAt: (productId: String) -> Unit,
    onOpenDetail: (productId: String) -> Unit,
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

    LaunchedEffect(gridState, products) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offsetPx) ->
            val product = products.getOrNull(index) ?: return@collect
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
                if (collectionLabel.isNotBlank()) {
                    Text(
                        text = collectionLabel.uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.5.sp,
                        letterSpacing = 0.08.em,
                        fontWeight = FontWeight.Medium,
                    )
                }
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
            items(products, key = { it.ref }) { product ->
                ProductCard(
                    product = product,
                    size = ProductCardSize.Md,
                    pointed = pointedRef == product.ref,
                    onClick = {
                        if (pointing) {
                            pointedRef = product.ref
                            onPointAt(product.ref)
                        } else {
                            onOpenDetail(product.ref)
                        }
                    },
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
                androidx.compose.material3.Icon(
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
    productByRef: Map<String, Product>,
    onDismiss: () -> Unit,
    onConfirmOrder: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val total = state.cart.sumOf { line ->
        (state.priceCentsByRef[line.productId] ?: 0L) * line.units
    }
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
                Text(
                    text = "Pedido em construção",
                    color = colors.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
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
                    items(state.cart, key = { it.productId + "/" + it.size }) { line ->
                        CartRow(
                            line = line,
                            product = productByRef[line.productId],
                            unitPriceCents = state.priceCentsByRef[line.productId] ?: 0L,
                        )
                    }
                }
                CartTotalBar(units = state.cart.sumOf { it.units }, totalCents = total)
                Spacer(modifier = Modifier.height(2.dp))
                Btn(
                    text = "Confirmar pedido · ${formatBrl(total)}",
                    onClick = onConfirmOrder,
                    kind = BtnKind.Jade,
                    size = BtnSize.Lg,
                    icon = TrovataIcons.check,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CartRow(line: CartLineUi, product: Product?, unitPriceCents: Long) {
    val colors = TrovataTokens.colors
    if (product == null) {
        Text(text = "${line.productId} · ${line.size} · ${line.units}un", color = colors.ink2)
        return
    }
    ProductRow(
        product = product,
        size = ProductRowSize.Md,
        quantity = line.units,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Tam ${line.size}",
                    color = colors.ink3,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatBrl(unitPriceCents * line.units),
                    color = colors.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
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
            androidx.compose.material3.Icon(
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
            text = "Quando o cliente adicionar uma peça, ela aparece aqui em tempo real.",
            color = colors.ink3,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
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

@Composable
private fun OrderSummaryOverlay(
    summary: OrderSummaryUi,
    productByRef: Map<String, Product>,
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
                OrderSummaryRow(line = line, product = productByRef[line.productId])
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
private fun OrderSummaryRow(line: OrderLine, product: Product?) {
    val colors = TrovataTokens.colors
    if (product == null) {
        Text(
            text = "${line.productId} · ${line.size} · ${line.units}un",
            color = colors.ink2,
        )
        return
    }
    ProductRow(
        product = product,
        size = ProductRowSize.Md,
        quantity = line.units,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Tam ${line.size}",
                    color = colors.ink3,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatBrl(line.subtotalCents),
                    color = colors.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}

private fun formatBrl(cents: Long): String {
    val whole = cents / 100
    val fractional = (cents % 100).toString().padStart(2, '0')
    val wholeWithDots = whole.toString().reversed().chunked(3).joinToString(".").reversed()
    return "R$ $wholeWithDots,$fractional"
}
