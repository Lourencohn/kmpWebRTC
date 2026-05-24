package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trovata.cast.theme.TrovataTokens

@Composable
fun HBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = TrovataTokens.colors.brand,
    track: Color = TrovataTokens.colors.surface2,
    height: Dp = 6.dp,
) {
    val pct = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(track, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pct)
                .fillMaxHeight()
                .background(color, shape),
        )
    }
}
