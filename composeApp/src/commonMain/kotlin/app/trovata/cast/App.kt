package app.trovata.cast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trovata.cast.navigation.AuthWelcomeRoute
import app.trovata.cast.theme.TrovataTheme
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.screens.tabs.TabsHostRoute
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition

@Composable
fun App() {
    val container = AppContainerHolder.current
    val isAuthenticated by container.authRepository.isAuthenticated.collectAsState()

    TrovataTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrovataTokens.colors.bg),
        ) {
            AnimatedContent(
                targetState = isAuthenticated,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "auth-root",
            ) { authenticated ->
                val root: Screen = if (authenticated) TabsHostRoute else AuthWelcomeRoute
                Navigator(root) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
