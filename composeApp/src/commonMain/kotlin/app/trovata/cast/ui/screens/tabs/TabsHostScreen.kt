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
import app.trovata.cast.navigation.AccountRoute
import app.trovata.cast.navigation.ProductDetailRoute
import app.trovata.cast.feature.catalog.CatalogScreenModel
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.dashboard.DashboardScreenModel
import app.trovata.cast.feature.insights.InsightsScreenModel
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.SellerTab
import app.trovata.cast.ui.components.TabBar
import app.trovata.cast.ui.screens.catalog.CatalogScreen
import app.trovata.cast.ui.screens.clients.ClientsScreen
import app.trovata.cast.ui.screens.dashboard.DashboardScreen
import app.trovata.cast.ui.screens.insights.InsightsScreen
import app.trovata.cast.ui.screens.invite.InviteScreen
import app.trovata.cast.ui.screens.prep.CatalogPickerScreen
import app.trovata.cast.ui.screens.sessions.SellerHomeScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

object TabsHostRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<SessionsViewModel>()
        val homeState by viewModel.home.collectAsState()
        val colors = TrovataTokens.colors
        val openAccount: () -> Unit = { navigator.push(AccountRoute) }

        val catalogModel = koinScreenModel<CatalogScreenModel>()
        val catalogState by catalogModel.state.collectAsState()

        val clientsModel = koinScreenModel<ClientsScreenModel>()
        val clientsState by clientsModel.state.collectAsState()

        val insightsModel = koinScreenModel<InsightsScreenModel>()
        val insightsState by insightsModel.state.collectAsState()

        val dashboardModel = koinScreenModel<DashboardScreenModel>()
        val dashboardState by dashboardModel.state.collectAsState()

        Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (homeState.activeTab) {
                    SellerTab.Painel -> DashboardScreen(
                        state = dashboardState,
                        onOpenAccount = openAccount,
                        onPeriodChange = dashboardModel::setPeriod,
                        onMetricChange = dashboardModel::setMetric,
                    )
                    SellerTab.Sessoes -> SellerHomeScreen(
                        viewModel = viewModel,
                        onInviteClient = { navigator.push(CatalogPickerScreen()) },
                        onOpenSession = { record -> navigator.push(InviteScreen(record)) },
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
                    SellerTab.Insights -> InsightsScreen(
                        state = insightsState,
                        onOpenAccount = openAccount,
                    )
                }
            }
            TabBar(active = homeState.activeTab, onSelect = viewModel::selectTab)
        }
    }
}
