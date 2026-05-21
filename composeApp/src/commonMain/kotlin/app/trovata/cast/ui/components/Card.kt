package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trovata.cast.theme.TrovataTokens

@Composable
fun TrovataCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    radius: Dp = 16.dp,
    elevated: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(radius)
    val base = modifier
    val shadowed = if (elevated) base.shadow(elevation = 1.dp, shape = shape, clip = false) else base
    Box(
        modifier = shadowed
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape)
            .padding(padding),
    ) {
        content()
    }
}
