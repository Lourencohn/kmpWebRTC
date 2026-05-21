package app.trovata.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.theme.TrovataTheme
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.screens.preview.DesignSystemScreen
import app.trovata.cast.ui.screens.sessions.IncomingCallScreen
import app.trovata.cast.ui.screens.sessions.SellerHomeScreen
import app.trovata.cast.ui.screens.sessions.SessionPrepScreen

private enum class DemoScreen(val label: String) {
    Home("Home"),
    Incoming("Atender"),
    Prep("Preparar"),
    Preview("Design"),
}

@Composable
fun App() {
    TrovataTheme {
        val viewModel = remember { SessionsViewModel() }
        var current by remember { mutableStateOf(DemoScreen.Home) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrovataTokens.colors.bg),
        ) {
            when (current) {
                DemoScreen.Home -> SellerHomeScreen(
                    viewModel = viewModel,
                    onOpenIncoming = { current = DemoScreen.Incoming },
                    onOpenPrep = { current = DemoScreen.Prep },
                    onInviteClient = { current = DemoScreen.Prep },
                )
                DemoScreen.Incoming -> IncomingCallScreen(
                    viewModel = viewModel,
                    onAnswered = { current = DemoScreen.Home },
                    onDeclined = { current = DemoScreen.Home },
                    onRescheduled = { current = DemoScreen.Home },
                )
                DemoScreen.Prep -> SessionPrepScreen(
                    viewModel = viewModel,
                    onBack = { current = DemoScreen.Home },
                    onStartCall = { current = DemoScreen.Home },
                )
                DemoScreen.Preview -> DesignSystemScreen()
            }

            DemoSwitcher(
                current = current,
                onSelect = { current = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun DemoSwitcher(
    current: DemoScreen,
    onSelect: (DemoScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .background(colors.ink.copy(alpha = 0.86f), shape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DemoScreen.entries.forEach { screen ->
            val active = screen == current
            Box(
                modifier = Modifier
                    .background(
                        if (active) Color.White else Color.Transparent,
                        shape,
                    )
                    .clickable { onSelect(screen) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = screen.label,
                    color = if (active) colors.ink else Color.White.copy(alpha = 0.8f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
