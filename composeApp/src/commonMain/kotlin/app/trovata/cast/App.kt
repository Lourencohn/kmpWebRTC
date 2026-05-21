package app.trovata.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trovata.cast.navigation.SellerHomeRoute
import app.trovata.cast.theme.TrovataTheme
import app.trovata.cast.theme.TrovataTokens
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition

@Composable
fun App() {
    TrovataTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TrovataTokens.colors.bg),
        ) {
            Navigator(SellerHomeRoute) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
