package app.trovata.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trovata.cast.theme.TrovataTheme
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Wordmark

@Composable
fun App() {
    TrovataTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrovataTokens.colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            Wordmark()
        }
    }
}
