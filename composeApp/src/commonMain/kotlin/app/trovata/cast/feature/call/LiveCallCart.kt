package app.trovata.cast.feature.call

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.format.formatBrl
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import coil3.compose.AsyncImage

@Composable
internal fun CartToastView(toast: CartToast, modifier: Modifier = Modifier) {
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
internal fun CartDrawer(
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
                .navigationBarsPadding()
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
        val clientName = state.cartClientName?.takeIf { it.isNotBlank() }
        if (clientName != null) "Carrinho de $clientName no Catálogo Link" else "Carrinho aberto no Catálogo Link"
    }
}

@Composable
internal fun CartErrorBanner(
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
