package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.theme.TrovataTokens
import org.koin.compose.koinInject

@Composable
fun AccountChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val user by koinInject<AuthRepository>().user.collectAsState()
    val name = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Conta"
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
            hue = avatarHue(name),
            size = 38.dp,
        )
    }
}

private fun avatarHue(seed: String): Double {
    val hash = seed.fold(0) { acc, c -> acc * 31 + c.code }
    return (((hash % 360) + 360) % 360).toDouble()
}
