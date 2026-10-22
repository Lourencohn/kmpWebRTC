package app.trovata.cast.feature.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.NumberedStep
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
internal fun HowItWorksCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape)
            .background(colors.surface, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(colors.live.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TrovataIcons.mic,
                    contentDescription = null,
                    tint = colors.live,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = "Como funciona a gravação",
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        NumberedStep(
            number = 1,
            title = "Escolha o ponto de partida",
            detail = "Navegue até onde quer começar. A apresentação abre exatamente aqui.",
        )
        NumberedStep(
            number = 2,
            title = "Grave falando e mostrando",
            detail = "Sua voz e cada toque na vitrine entram juntos. Não é vídeo: o cliente vê a vitrine de verdade.",
        )
        NumberedStep(
            number = 3,
            title = "Envie o link",
            detail = "O cliente abre no navegador e assiste quando puder.",
        )
        Btn(
            text = "Entendi",
            onClick = onDismiss,
            kind = BtnKind.Soft,
            size = BtnSize.Sm,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
internal fun HowItWorksChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .shadow(4.dp, shape)
            .background(colors.surface, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.mic,
            contentDescription = null,
            tint = colors.live,
            modifier = Modifier.size(14.dp),
        )
        Text(text = "Como funciona?", color = colors.ink, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    }
}
