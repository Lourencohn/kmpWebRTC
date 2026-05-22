package app.trovata.cast.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.AppContainerHolder
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
        )
    }
}

@Composable
private fun LiveCallBody(
    state: LiveCallUiState,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val ink = colors.ink
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ink),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusPill(state = state)
            Spacer(modifier = Modifier.height(28.dp))
            CallAvatar(state = state)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (state.isLive) "Conectado" else if (state.isNegotiating) "Conectando…" else "Aguardando cliente",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).em,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage ?: "Token ${state.token}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )

            if (state.isLive && state.remoteMuted) {
                Spacer(modifier = Modifier.height(12.dp))
                Pill(
                    text = "Cliente mutou o microfone",
                    tone = PillTone.Dark,
                    icon = TrovataIcons.micOff,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sinalização",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = state.signaling::class.simpleName ?: "?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Peer",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = state.peer::class.simpleName ?: "?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(
                    icon = if (state.localMuted) TrovataIcons.micOff else TrovataIcons.mic,
                    onClick = onToggleMute,
                    kind = IconBtnKind.Dark,
                    active = state.localMuted,
                    size = 56.dp,
                    contentDescription = if (state.localMuted) "Reativar microfone" else "Silenciar microfone",
                )
                Spacer(modifier = Modifier.width(12.dp))
                Btn(
                    text = "Encerrar",
                    onClick = onHangup,
                    kind = BtnKind.Danger,
                    size = BtnSize.Lg,
                    icon = TrovataIcons.hangup,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
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
            text = state.role.name.first().toString(),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
