package app.trovata.cast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.sync.ClientSyncCoordinator
import app.trovata.cast.feature.auth.AuthWelcomeRoute
import app.trovata.cast.feature.tabs.TabsHostRoute
import app.trovata.cast.ui.theme.TrovataTheme
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        val authRepository = koinInject<AuthRepository>()
        val syncCoordinator = koinInject<ClientSyncCoordinator>()
        val isAuthenticated by authRepository.isAuthenticated.collectAsState()

        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .crossfade(true)
                .build()
        }

        LaunchedEffect(isAuthenticated) {
            if (isAuthenticated) syncCoordinator.start()
        }

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
}
