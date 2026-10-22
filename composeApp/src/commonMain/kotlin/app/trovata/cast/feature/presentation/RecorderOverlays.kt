package app.trovata.cast.feature.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.coroutines.delay

private const val ActionToastMs: Long = 1_600

@Composable
internal fun RecordedActionToast(action: RecordedAction?, capturing: Boolean, modifier: Modifier = Modifier) {
    var label by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(action?.sequence) {
        val current = action ?: return@LaunchedEffect
        label = current.label
        visible = true
        delay(ActionToastMs)
        visible = false
    }
    AnimatedVisibility(
        visible = visible && capturing,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(999.dp))
                .background(TrovataTokens.colors.ink.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = TrovataIcons.check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "Gravado",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun CountdownOverlay(seconds: Int, onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "A gravação começa em", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
            AnimatedContent(
                targetState = seconds,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 1.6f)) togetherWith fadeOut(tween(160))
                },
                label = "countdown",
            ) { value ->
                Text(text = "$value", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Prepare-se para falar. Toque para cancelar.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun PausedBanner(modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(colors.warn, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.pause,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Gravação pausada. O microfone está desligado até você tocar em Retomar.",
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
        )
    }
}

@Composable
internal fun RecorderErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    Row(
        modifier = modifier
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
            text = message,
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
