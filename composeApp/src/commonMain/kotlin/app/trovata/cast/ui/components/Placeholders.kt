package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trovata.cast.theme.TrovataTokens

@Composable
fun PlaceholderBar(
    modifier: Modifier = Modifier,
    height: Dp = 11.dp,
    color: Color = TrovataTokens.colors.surface2,
) {
    Box(
        modifier = modifier
            .height(height)
            .background(color, RoundedCornerShape(999.dp)),
    )
}
