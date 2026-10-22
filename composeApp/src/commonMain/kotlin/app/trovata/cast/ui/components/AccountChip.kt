package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
fun AccountChip(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Box(
        modifier = modifier
            .shadow(1.dp, CircleShape, clip = false)
            .background(colors.surface, CircleShape)
            .border(1.dp, colors.line, CircleShape)
            .size(38.dp)
            .clickable(onClick = onClick),
    ) {
        Avatar(
            name = name,
            hue = avatarHueFor(name),
            size = 38.dp,
        )
    }
}
