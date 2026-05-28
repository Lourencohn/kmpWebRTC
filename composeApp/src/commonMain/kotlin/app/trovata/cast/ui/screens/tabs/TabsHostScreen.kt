package app.trovata.cast.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.navigation.AccountRoute
import app.trovata.cast.navigation.IncomingCallRoute
import app.trovata.cast.navigation.ProductDetailRoute
import app.trovata.cast.navigation.SessionPrepRoute
import app.trovata.cast.feature.catalog.CatalogScreenModel
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.SellerTab
import app.trovata.cast.ui.components.TabBar
import app.trovata.cast.ui.screens.catalog.CatalogScreen
import app.trovata.cast.ui.screens.clients.ClientsScreen
import app.trovata.cast.ui.screens.insights.InsightsScreen
import app.trovata.cast.ui.screens.prep.CatalogPickerScreen
import app.trovata.cast.ui.screens.sessions.SellerHomeScreen
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object TabsHostRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = container.sessionsViewModel
        val homeState by viewModel.home.collectAsState()
        val colors = TrovataTokens.colors
        val openAccount: () -> Unit = { navigator.push(AccountRoute) }

        val catalogModel = rememberScreenModel(tag = "catalog") {
            CatalogScreenModel(container.catalogRepository, container.authRepository)
        }
        val catalogState by catalogModel.state.collectAsState()

        val clientsModel = rememberScreenModel(tag = "clients") {
            ClientsScreenModel(container.clientsRepository)
        }
        val clientsState by clientsModel.state.collectAsState()

        Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (homeState.activeTab) {
                    SellerTab.Sessoes -> SellerHomeScreen(
                        viewModel = viewModel,
                        onOpenIncoming = { navigator.push(IncomingCallRoute) },
                        onOpenPrep = { upcoming ->
                            navigator.push(SessionPrepRoute(clientName = upcoming.client.name))
                        },
                        onInviteClient = { navigator.push(CatalogPickerScreen()) },
                        onOpenAccount = openAccount,
                    )
                    SellerTab.Catalogo -> CatalogScreen(
                        state = catalogState,
                        onOpenAccount = openAccount,
                        onOpenProduct = { product ->
                            navigator.push(ProductDetailRoute(productRef = product.ref))
                        },
                        onPrevPage = catalogModel::prevPage,
                        onNextPage = catalogModel::nextPage,
                    )
                    SellerTab.Clientes -> ClientsScreen(
                        state = clientsState,
                        onQueryChange = clientsModel::setQuery,
                        onOpenAccount = openAccount,
                        onInviteClient = { client ->
                            navigator.push(CatalogPickerScreen(clientName = client.name))
                        },
                    )
                    SellerTab.Insights -> InsightsScreen(onOpenAccount = openAccount)
                }
            }
            TabBar(active = homeState.activeTab, onSelect = viewModel::selectTab)
        }
    }
}
