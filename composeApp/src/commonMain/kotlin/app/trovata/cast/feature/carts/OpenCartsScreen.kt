package app.trovata.cast.feature.carts

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.CenteredMessage
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.SelectableChip
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
fun OpenCartsTab(
    screenModel: OpenCartsScreenModel,
    accountName: String,
    onOpenAccount: () -> Unit,
    onSessionCreated: (StoredSessionRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()

    LaunchedEffect(state.createdSession?.sessionId) {
        val record = state.createdSession ?: return@LaunchedEffect
        screenModel.consumeCreatedSession()
        onSessionCreated(record)
    }

    OpenCartsBody(
        modifier = modifier,
        state = state,
        accountName = accountName,
        onOpenAccount = onOpenAccount,
        onRefresh = screenModel::refresh,
        onFilter = screenModel::setFilter,
        onToggleOrder = screenModel::toggleOrder,
        onSearchChange = screenModel::setSearch,
        onSearchSubmit = screenModel::submitSearch,
        onDismissError = screenModel::clearError,
        onCallLive = screenModel::callLive,
    )
}

@Composable
fun OpenCartsBody(
    state: OpenCartsUiState,
    accountName: String,
    onOpenAccount: () -> Unit,
    onRefresh: () -> Unit,
    onFilter: (OpenCartsFilter) -> Unit,
    onToggleOrder: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onDismissError: () -> Unit,
    onCallLive: (OpenCart) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val visible = state.visibleCarts

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabHeader(
                modifier = Modifier.padding(top = 56.dp),
                eyebrow = state.companyName.ifBlank { "Catálogo Link" },
                title = "Carrinhos",
                subtitle = headerSubtitle(state),
                accountName = accountName,
                onOpenAccount = onOpenAccount,
                secondaryIcon = TrovataIcons.refresh,
                onSecondaryClick = onRefresh,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = state.search,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.ink, fontSize = 13.sp),
                    cursorBrush = SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.surface, RoundedCornerShape(999.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (state.search.isEmpty()) {
                            Text(
                                text = "Buscar por cliente ou e-mail",
                                color = colors.ink4,
                                fontSize = 13.sp,
                            )
                        }
                        inner()
                    },
                )
                Btn(text = "Buscar", onClick = onSearchSubmit, kind = BtnKind.Surface, size = BtnSize.Sm)
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(OpenCartsFilter.entries.toList()) { filter ->
                    SelectableChip(
                        label = filter.label,
                        active = state.filter == filter,
                        onClick = { onFilter(filter) },
                    )
                }
                item {
                    SelectableChip(
                        label = state.order.label,
                        active = false,
                        onClick = onToggleOrder,
                    )
                }
            }

            when {
                state.isLoading -> CenteredMessage("Buscando carrinhos abertos...")
                visible.isEmpty() && state.error == null -> EmptyCarts(
                    filter = state.filter,
                    onRetry = onRefresh,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.carrinhoId }) { cart ->
                        OpenCartRow(
                            cart = cart,
                            nowMs = state.nowMs,
                            calling = state.callingCartId == cart.carrinhoId,
                            busy = state.callingCartId != null,
                            onCallLive = { onCallLive(cart) },
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Text(
                                text = "Mostrando ${visible.size} de ${state.total}. Use a busca para achar outro.",
                                color = colors.ink4,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
            ) {
                CartErrorBanner(text = error, onDismiss = onDismissError)
            }
        }
    }
}

private fun headerSubtitle(state: OpenCartsUiState): String {
    if (state.isLoading) return "Carregando carrinhos..."
    val resumable = state.resumable
    if (resumable.isEmpty()) return "Nenhum carrinho para retomar agora"
    return "${resumable.size} para retomar · ${formatBrl(state.resumableTotalCents)} parados"
}

@Composable
private fun OpenCartRow(
    cart: OpenCart,
    nowMs: Long,
    calling: Boolean,
    busy: Boolean,
    onCallLive: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    val blockReason = cart.callBlockReason(nowMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cart.title,
                    color = colors.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                cart.catalogName?.let { catalogName ->
                    Text(text = catalogName, color = colors.ink3, fontSize = 12.sp)
                }
            }
            Pill(
                text = cart.status.label,
                tone = if (cart.status == CartStatus.Editing) PillTone.Jade else PillTone.Neutral,
            )
        }

        Text(
            text = listOfNotNull(
                "${cart.itemCount} ${if (cart.itemCount == 1) "item" else "itens"}",
                cart.totalCents?.let { formatBrl(it) },
                idleTimeLabel(cart.updatedAtMs, nowMs),
            ).joinToString(" · "),
            color = colors.ink4,
            fontSize = 11.5.sp,
        )

        if (blockReason == null) {
            Btn(
                text = if (calling) "Abrindo sessão..." else "Chamar ao vivo",
                onClick = onCallLive,
                kind = BtnKind.Jade,
                size = BtnSize.Sm,
                icon = TrovataIcons.video,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(text = blockReason, color = colors.ink3, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun EmptyCarts(filter: OpenCartsFilter, onRetry: () -> Unit) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (filter == OpenCartsFilter.Resumable) {
                "Nenhum carrinho parado para retomar"
            } else {
                "Nenhum carrinho encontrado"
            },
            color = colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (filter == OpenCartsFilter.Resumable) {
                "Quando um cliente montar um carrinho e não fechar, ele aparece aqui para você chamar ao vivo."
            } else {
                "Ainda não há carrinhos nos seus catálogos link."
            },
            color = colors.ink3,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Btn(text = "Atualizar", onClick = onRetry, kind = BtnKind.Surface, size = BtnSize.Sm)
    }
}

@Composable
private fun CartErrorBanner(text: String, onDismiss: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.live, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.bell,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "OK",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
