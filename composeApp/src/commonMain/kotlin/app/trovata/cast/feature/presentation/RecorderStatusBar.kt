package app.trovata.cast.feature.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

private val MeterBarWeights = listOf(0.45f, 0.8f, 1f, 0.7f, 0.4f)

@Composable
internal fun RecorderTopBar(state: PresentationRecorderUiState, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f))
            .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBtn(
                icon = TrovataIcons.back,
                onClick = onClose,
                kind = IconBtnKind.Dark,
                size = 36.dp,
                contentDescription = "Sair da gravação",
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gravar apresentação",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.catalogoNome,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusCapsule(state = state)
            Spacer(modifier = Modifier.weight(1f))
            if (state.isCapturing) {
                Text(
                    text = if (state.eventCount == 1) "1 ação" else "${state.eventCount} ações",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Text(
            text = hintFor(state),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun StatusCapsule(state: PresentationRecorderUiState) {
    val colors = TrovataTokens.colors
    val neutral = Color.White.copy(alpha = 0.10f)
    val (background, label) = when (state.phase) {
        RecorderPhase.Preparing -> neutral to "Pronto para gravar"
        RecorderPhase.Countdown -> colors.live.copy(alpha = 0.35f) to "Começando em ${state.countdown}"
        RecorderPhase.Recording -> colors.live to "GRAVANDO"
        RecorderPhase.Paused -> colors.warn to "PAUSADO"
        RecorderPhase.Finishing -> neutral to "Finalizando"
        RecorderPhase.Review -> neutral to "Gravação concluída"
        RecorderPhase.Uploading -> colors.brand to "Enviando"
        RecorderPhase.Shared -> colors.jade to "Enviada"
    }
    val dotColor = when (state.phase) {
        RecorderPhase.Preparing, RecorderPhase.Finishing, RecorderPhase.Review -> colors.live
        else -> Color.White
    }
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RecordDot(color = dotColor, pulsing = state.phase == RecorderPhase.Recording)
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.04.em,
        )
        if (state.isCapturing) {
            Text(
                text = formatClock(state.elapsedMs),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (state.phase == RecorderPhase.Recording) MicLevelMeter(level = state.micLevel)
    }
}

@Composable
private fun RecordDot(color: Color, pulsing: Boolean, size: Dp = 9.dp) {
    val alpha = if (pulsing) rememberPulse(from = 1f, to = 0.25f, durationMs = 700) else 1f
    Box(
        modifier = Modifier
            .size(size)
            .alpha(alpha)
            .background(color, CircleShape),
    )
}

@Composable
private fun MicLevelMeter(level: Float) {
    val animated by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(durationMillis = 90),
        label = "mic-level",
    )
    Row(
        modifier = Modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeterBarWeights.forEach { weight ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(3.dp + 13.dp * (animated * weight))
                    .background(Color.White, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
internal fun rememberPulse(from: Float, to: Float, durationMs: Int): Float {
    val transition = rememberInfiniteTransition(label = "recorder-pulse")
    val value by transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(animation = tween(durationMs), repeatMode = RepeatMode.Reverse),
        label = "recorder-pulse-value",
    )
    return value
}

@Composable
internal fun RecordingFrame(phase: RecorderPhase) {
    val colors = TrovataTokens.colors
    val color = when (phase) {
        RecorderPhase.Recording -> colors.live
        RecorderPhase.Paused -> colors.warn
        else -> return
    }
    val alpha = if (phase == RecorderPhase.Recording) rememberPulse(from = 1f, to = 0.45f, durationMs = 900) else 1f
    Box(modifier = Modifier.fillMaxSize().border(3.dp, color.copy(alpha = alpha)))
}

private fun hintFor(state: PresentationRecorderUiState): String = when (state.phase) {
    RecorderPhase.Preparing ->
        "Deixe a vitrine onde quer começar e toque no botão vermelho. Sua voz e tudo que você fizer na vitrine entram na gravação."
    RecorderPhase.Countdown -> "Prepare-se para falar."
    RecorderPhase.Recording ->
        "Fale e mostre: abra categorias e produtos, desenhe e mude quantidades. Limite de ${MaxPresentationMs / 60_000} minutos."
    RecorderPhase.Paused -> "O que você fizer na vitrine agora aparece no ponto em que retomar."
    RecorderPhase.Finishing -> "Fechando o áudio..."
    RecorderPhase.Review, RecorderPhase.Uploading -> "Confira e envie a apresentação."
    RecorderPhase.Shared -> "Link pronto para mandar aos clientes."
}

internal fun formatClock(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
