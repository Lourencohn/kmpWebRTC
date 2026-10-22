package app.trovata.cast.feature.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
internal fun RecorderActionBar(
    state: PresentationRecorderUiState,
    onStart: () -> Unit,
    onCancelCountdown: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onToggleDrawing: () -> Unit,
    onClearDrawing: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f))
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)
            .heightIn(min = 92.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state.phase) {
            RecorderPhase.Preparing -> RecordButton(label = "Toque para gravar", armed = false, onClick = onStart)
            RecorderPhase.Countdown -> RecordButton(label = "Cancelar", armed = true, onClick = onCancelCountdown)
            RecorderPhase.Recording, RecorderPhase.Paused -> CaptureControls(
                state = state,
                onPause = onPause,
                onResume = onResume,
                onFinish = onFinish,
                onToggleDrawing = onToggleDrawing,
                onClearDrawing = onClearDrawing,
            )
            else -> Unit
        }
    }
}

@Composable
private fun RecordButton(label: String, armed: Boolean, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val innerSize by animateDpAsState(targetValue = if (armed) 26.dp else 52.dp, label = "record-inner-size")
    val innerRadius by animateDpAsState(targetValue = if (armed) 7.dp else 26.dp, label = "record-inner-radius")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                .clickable(onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .background(colors.live, RoundedCornerShape(innerRadius)),
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CaptureControls(
    state: PresentationRecorderUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onToggleDrawing: () -> Unit,
    onClearDrawing: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.phase == RecorderPhase.Recording) {
            ControlButton(icon = TrovataIcons.pause, label = "Pausar", kind = IconBtnKind.Dark, onClick = onPause)
        } else {
            ControlButton(icon = TrovataIcons.play, label = "Retomar", kind = IconBtnKind.Jade, onClick = onResume)
        }
        ControlButton(
            icon = TrovataIcons.pointer,
            label = if (state.drawing) "Desenhando" else "Desenhar",
            kind = if (state.drawing) IconBtnKind.Brand else IconBtnKind.Dark,
            onClick = onToggleDrawing,
        )
        if (state.drawing) {
            ControlButton(icon = TrovataIcons.trash, label = "Apagar", kind = IconBtnKind.Dark, onClick = onClearDrawing)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Btn(
                text = "Concluir",
                onClick = onFinish,
                kind = BtnKind.Jade,
                size = BtnSize.Md,
                icon = TrovataIcons.stop,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Encerra e revisa",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, kind: IconBtnKind, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconBtn(icon = icon, onClick = onClick, kind = kind, size = 44.dp, contentDescription = label)
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
