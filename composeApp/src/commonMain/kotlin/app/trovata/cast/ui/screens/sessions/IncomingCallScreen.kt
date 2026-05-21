package app.trovata.cast.ui.screens.sessions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.color.oklch
import app.trovata.cast.ui.components.Avatar
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun IncomingCallScreen(
    viewModel: SessionsViewModel,
    modifier: Modifier = Modifier,
    onAnswered: () -> Unit = {},
    onDeclined: () -> Unit = {},
    onRescheduled: () -> Unit = {},
) {
    val state by viewModel.incoming.collectAsState()
    val colors = TrovataTokens.colors
    val client = state.session.client

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink),
    ) {
        Spacer(modifier = Modifier.height(72.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Pill(text = "Chamada recebida", tone = PillTone.Live, icon = TrovataIcons.bell)
            Text(
                text = "TrovataCast está tocando",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PulsingAvatar(name = client.name, hue = client.hue)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = client.name,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.02).em,
                )
                Text(
                    text = "${client.shop} · ${client.city}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${client.previousSessions} sessões anteriores · ${state.session.viewingHint}",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        when {
            state.accepted -> StatusBanner(
                label = "Atendendo Diego…",
                bg = colors.jade,
            )
            state.declined -> StatusBanner(
                label = "Chamada recusada",
                bg = colors.live,
            )
            state.rescheduled -> StatusBanner(
                label = "Sugerindo novo horário",
                bg = colors.brand,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Btn(
                text = "Recusar",
                onClick = {
                    viewModel.declineIncoming()
                    onDeclined()
                },
                kind = BtnKind.Danger,
                size = BtnSize.Lg,
                icon = TrovataIcons.hangup,
                modifier = Modifier.weight(1f),
            )
            Btn(
                text = "Atender",
                onClick = {
                    viewModel.acceptIncoming()
                    onAnswered()
                },
                kind = BtnKind.Jade,
                size = BtnSize.Lg,
                icon = TrovataIcons.video,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            RescheduleAction(onClick = {
                viewModel.rescheduleIncoming()
                onRescheduled()
            })
        }

        Spacer(modifier = Modifier.height(42.dp))
    }
}

@Composable
private fun PulsingAvatar(name: String, hue: Double) {
    val transition = rememberInfiniteTransition(label = "incoming-pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100), repeatMode = RepeatMode.Reverse),
        label = "incoming-pulse-scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100), repeatMode = RepeatMode.Reverse),
        label = "incoming-pulse-alpha",
    )
    val ringColor = oklch(70.0, 0.12, hue, alpha = alpha)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        Box(
            modifier = Modifier
                .size(160.dp * scale)
                .background(ringColor, CircleShape),
        )
        Avatar(name = name, hue = hue, size = 120.dp)
    }
}

@Composable
private fun StatusBanner(label: String, bg: Color) {
    val opacity by animateFloatAsState(targetValue = 1f, label = "status-banner-fade")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .background(bg.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .border(1.dp, bg.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = opacity),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RescheduleAction(onClick: () -> Unit) {
    Btn(
        text = "Reagendar para mais tarde",
        onClick = onClick,
        kind = BtnKind.Dark,
        size = BtnSize.Md,
        icon = TrovataIcons.clock,
        modifier = Modifier.fillMaxWidth(),
    )
}

