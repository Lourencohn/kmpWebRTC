package app.trovata.cast.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import app.trovata.cast.webrtc.PeerSessionState

@Composable
internal fun CallTopBar(state: LiveCallUiState, clientName: String?) {
    val colors = TrovataTokens.colors
    val name = clientName?.takeIf { it.isNotBlank() }
        ?: state.cartClientName?.takeIf { it.isNotBlank() }
        ?: "Atendimento"
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Avatar(name = name, size = 34.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = callStatusMessage(state),
                    color = colors.ink3,
                    fontSize = 12.sp,
                )
            }
            Pill(
                text = when {
                    state.errorMessage != null -> "Sem conexão"
                    state.peer is PeerSessionState.Closed -> "Encerrada"
                    state.isLive -> "Ao vivo"
                    state.isNegotiating -> "Conectando"
                    else -> "Aguardando"
                },
                tone = when {
                    state.errorMessage != null -> PillTone.Live
                    state.isLive -> PillTone.Jade
                    else -> PillTone.Neutral
                },
            )
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = colors.live,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            )
        }
        HorizontalDivider(color = colors.line)
    }
}

private fun callStatusMessage(state: LiveCallUiState): String = when {
    state.errorMessage != null -> "Verifique sua conexão"
    state.peer is PeerSessionState.Closed -> "O cliente saiu da chamada"
    state.isLive && state.remoteMuted -> "Microfone do cliente desligado"
    state.isLive -> "Vitrine compartilhada"
    state.isNegotiating -> "Conectando com o cliente"
    else -> "Aguardando o cliente entrar"
}

@Composable
internal fun CallActionBar(
    state: LiveCallUiState,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleDrawing: () -> Unit,
    onClearDrawing: () -> Unit,
    onOpenCart: () -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface).navigationBarsPadding()) {
        HorizontalDivider(color = colors.line)
        if (state.drawing) {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.brandTint).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Desenhe sobre o produto.\nToque em Navegar para rolar.",
                    color = colors.brand2,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearDrawing, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(TrovataIcons.trash, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Limpar", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallControl(
                label = if (state.localMuted) "Mudo" else "Microfone",
                icon = if (state.localMuted) TrovataIcons.micOff else TrovataIcons.mic,
                description = if (state.localMuted) "Reativar microfone" else "Silenciar microfone",
                stateLabel = if (state.localMuted) "Microfone desligado" else "Microfone ligado",
                active = state.localMuted,
                onClick = onToggleMute,
                modifier = Modifier.weight(1f),
            )
            CallControl(
                label = if (state.drawing) "Navegar" else "Apontar",
                icon = if (state.drawing) TrovataIcons.grid else TrovataIcons.pointer,
                description = if (state.drawing) "Parar de desenhar e navegar" else "Desenhar para o cliente",
                stateLabel = if (state.drawing) "Desenho ativado" else "Navegação ativada",
                active = state.drawing,
                onClick = onToggleDrawing,
                modifier = Modifier.weight(1f),
            )
            CallControl(
                label = "Carrinho",
                icon = TrovataIcons.cart,
                description = "Abrir carrinho, ${state.cartCount} unidades",
                count = state.cartCount,
                onClick = onOpenCart,
                modifier = Modifier.weight(1f),
            )
            CallControl(
                label = "Encerrar",
                icon = TrovataIcons.hangup,
                description = "Encerrar atendimento",
                danger = true,
                onClick = onHangup,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CallControl(
    label: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    danger: Boolean = false,
    count: Int = 0,
    stateLabel: String? = null,
) {
    val colors = TrovataTokens.colors
    val foreground = when {
        danger -> colors.live
        active -> colors.brand
        else -> colors.ink2
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateLabel?.let { stateDescription = it }
            }
            .heightIn(min = 68.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 36.dp)
                .background(
                    when {
                        danger -> colors.live.copy(alpha = 0.08f)
                        active -> colors.brandTint
                        else -> colors.surface2
                    },
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(badge = {
                if (count > 0) {
                    Badge(containerColor = colors.jade, contentColor = Color.White) {
                        Text(if (count > 99) "99+" else count.toString())
                    }
                }
            }) {
                Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(22.dp))
            }
        }
        Text(label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}
