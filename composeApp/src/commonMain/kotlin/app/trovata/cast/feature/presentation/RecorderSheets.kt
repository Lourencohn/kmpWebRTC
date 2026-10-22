package app.trovata.cast.feature.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens
import kotlinx.coroutines.delay

@Composable
internal fun ReviewSheet(
    state: PresentationRecorderUiState,
    onPublish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = TrovataTokens.colors
    val busy = state.phase != RecorderPhase.Review
    SheetScaffold {
        Pill(text = "Gravação concluída", tone = PillTone.Jade, icon = TrovataIcons.check)
        Text(
            text = "Sua apresentação está pronta",
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(label = "Duração", value = formatClock(state.elapsedMs), modifier = Modifier.weight(1f))
            StatTile(label = "Ações na vitrine", value = state.eventCount.toString(), modifier = Modifier.weight(1f))
        }
        ClientViewNote()
        if (state.phase == RecorderPhase.Uploading) UploadProgress(step = state.uploadStep)
        state.error?.let { message ->
            Text(text = message, color = colors.live, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Btn(
            text = when (state.phase) {
                RecorderPhase.Finishing -> "Preparando..."
                RecorderPhase.Uploading -> "Enviando..."
                else -> if (state.error != null) "Tentar enviar de novo" else "Enviar e gerar link"
            },
            onClick = onPublish,
            kind = if (busy) BtnKind.Soft else BtnKind.Primary,
            size = BtnSize.Lg,
            icon = TrovataIcons.send,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Btn(
            text = "Descartar e gravar de novo",
            onClick = onDiscard,
            kind = BtnKind.Ghost,
            size = BtnSize.Sm,
            icon = TrovataIcons.refresh,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .background(colors.surface2, shape)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = label, color = colors.ink4, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            color = colors.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun ClientViewNote() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.brandTint, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.eye,
            contentDescription = null,
            tint = colors.brand2,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Como o cliente vê", color = colors.brand2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Ele abre o link no navegador, toca em Assistir e ouve sua voz enquanto a vitrine se mexe sozinha. Preço e estoque são os do dia em que ele abrir.",
                color = colors.ink2,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun UploadProgress(step: UploadStep?) {
    val current = step ?: UploadStep.Creating
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UploadStep.entries.forEach { item ->
            UploadStepRow(
                label = item.label(),
                done = item.ordinal < current.ordinal,
                active = item == current,
            )
        }
    }
}

@Composable
private fun UploadStepRow(label: String, done: Boolean, active: Boolean) {
    val colors = TrovataTokens.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .alpha(if (active) rememberPulse(from = 1f, to = 0.4f, durationMs = 600) else 1f)
                .background(
                    color = when {
                        done -> colors.jade
                        active -> colors.brand
                        else -> Color.Transparent
                    },
                    shape = CircleShape,
                )
                .border(1.5.dp, if (done || active) Color.Transparent else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = TrovataIcons.check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        Text(
            text = label,
            color = if (done || active) colors.ink else colors.ink4,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun UploadStep.label(): String = when (this) {
    UploadStep.Creating -> "Criando a apresentação"
    UploadStep.Audio -> "Enviando o áudio"
    UploadStep.Timeline -> "Enviando os movimentos da vitrine"
}

@Composable
internal fun SharedSheet(
    state: PresentationRecorderUiState,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onRecordAnother: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = TrovataTokens.colors
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(CopiedFeedbackMs)
            copied = false
        }
    }
    val copy = {
        onCopy()
        copied = true
    }
    SheetScaffold {
        Pill(text = "Apresentação enviada", tone = PillTone.Jade, icon = TrovataIcons.check)
        Text(
            text = "Mande para os clientes",
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
        )
        Text(
            text = "O mesmo link serve para quantos clientes você quiser. Cada um assiste quando puder.",
            color = colors.ink3,
            fontSize = 13.sp,
        )
        RecordingsNote(saved = state.savedToRecordings)
        Text(
            text = state.shareUrl.orEmpty(),
            color = colors.ink2,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface2, RoundedCornerShape(10.dp))
                .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                .clickable(onClick = copy)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(
                text = if (copied) "Copiado" else "Copiar",
                onClick = copy,
                kind = BtnKind.Surface,
                size = BtnSize.Md,
                icon = if (copied) TrovataIcons.check else TrovataIcons.copy,
            )
            Btn(
                text = "Compartilhar",
                onClick = onShare,
                kind = BtnKind.Jade,
                size = BtnSize.Md,
                icon = TrovataIcons.share,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(
                text = "Gravar outra",
                onClick = onRecordAnother,
                kind = BtnKind.Ghost,
                size = BtnSize.Sm,
                icon = TrovataIcons.mic,
            )
            Spacer(modifier = Modifier.weight(1f))
            Btn(text = "Concluir", onClick = onClose, kind = BtnKind.Ghost, size = BtnSize.Sm)
        }
    }
}

@Composable
private fun RecordingsNote(saved: Boolean) {
    val colors = TrovataTokens.colors
    val tint = if (saved) colors.jade2 else colors.warn
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (saved) colors.jadeTint else colors.warn.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (saved) TrovataIcons.record else TrovataIcons.bell,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = if (saved) {
                "Guardada na aba Gravações. Você pode copiar ou reenviar o link de lá quando quiser."
            } else {
                "Não consegui guardar na aba Gravações. Copie ou compartilhe o link agora."
            },
            color = colors.ink2,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SheetScaffold(content: @Composable ColumnScope.() -> Unit) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = false) { },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bg, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.line, RoundedCornerShape(2.dp)),
            )
            content()
        }
    }
}
