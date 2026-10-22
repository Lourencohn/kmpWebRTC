package app.trovata.cast.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trovata.cast.feature.call.LiveWebBridge

@Composable
expect fun LiveCatalogWebView(
    url: String,
    bridge: LiveWebBridge,
    modifier: Modifier = Modifier,
)
