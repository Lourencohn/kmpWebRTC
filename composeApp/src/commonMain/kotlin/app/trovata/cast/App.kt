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
import app.trovata.cast.data.sync.CatalogSyncCoordinator
import app.trovata.cast.navigation.AuthWelcomeRoute
import app.trovata.cast.theme.TrovataTheme
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.screens.tabs.TabsHostRoute
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinContext {
        val authRepository = koinInject<AuthRepository>()
        val syncCoordinator = koinInject<CatalogSyncCoordinator>()
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
