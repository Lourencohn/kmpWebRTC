package app.trovata.cast.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.theme.TrovataTokens

@Composable
fun CenteredMessage(text: String) {
    val colors = TrovataTokens.colors
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colors.ink3, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
