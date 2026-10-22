package app.trovata.cast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trovata.cast.resources.Res
import app.trovata.cast.resources.trovata_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun Wordmark(modifier: Modifier = Modifier, height: Dp = 36.dp) {
    Image(
        painter = painterResource(Res.drawable.trovata_logo),
        contentDescription = "Trovata",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height),
    )
}
