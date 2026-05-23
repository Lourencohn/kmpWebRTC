package app.trovata.cast.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens

enum class PillTone { Neutral, Brand, Jade, Live, Warn, Ghost, Dark }

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Neutral,
    icon: ImageVector? = null,
) {
    val colors = TrovataTokens.colors
    val palette = when (tone) {
        PillTone.Neutral -> PillPalette(colors.surface2, colors.ink2, colors.line)
        PillTone.Brand -> PillPalette(colors.brandTint, colors.brand2, Color.Transparent)
        PillTone.Jade -> PillPalette(colors.jadeTint, colors.jade2, Color.Transparent)
        PillTone.Live -> PillPalette(colors.live.copy(alpha = 0.10f), colors.live, Color.Transparent)
        PillTone.Warn -> PillPalette(colors.warn.copy(alpha = 0.12f), colors.warn, Color.Transparent)
        PillTone.Ghost -> PillPalette(Color.Transparent, colors.ink3, colors.line)
        PillTone.Dark -> PillPalette(colors.ink.copy(alpha = 0.92f), Color.White, Color.Transparent)
    }
    val shape = RoundedCornerShape(999.dp)

    val pulseAlpha = if (tone == PillTone.Live) rememberLivePulse() else 1f

    Row(
        modifier = modifier
            .alpha(pulseAlpha)
            .background(palette.background, shape)
            .border(1.dp, palette.border, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.content,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text,
            color = palette.content,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.005).sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private data class PillPalette(
    val background: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun rememberLivePulse(): Float {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live-pulse-alpha",
    )
    return pulse
}
