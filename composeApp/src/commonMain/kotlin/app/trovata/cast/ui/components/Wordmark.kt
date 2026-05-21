package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import app.trovata.cast.theme.TrovataTokens

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val type = TrovataTokens.type
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.brand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.jade, CircleShape),
            )
        }
        Text(
            text = wordmarkLabel(colors.ink, colors.brand),
            style = type.title,
        )
    }
}

private fun wordmarkLabel(ink: androidx.compose.ui.graphics.Color, brand: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) { append("Trovata") }
        withStyle(SpanStyle(color = brand, fontWeight = FontWeight.SemiBold)) { append("Cast") }
    }
