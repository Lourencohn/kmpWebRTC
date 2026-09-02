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
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.navigation.AccountRoute
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.SellerTab
import app.trovata.cast.ui.components.TabBar
import app.trovata.cast.ui.screens.catalog.CatalogLinksTab
import app.trovata.cast.ui.screens.clients.ClientsScreen
import app.trovata.cast.ui.screens.invite.InviteScreen
import app.trovata.cast.ui.screens.prep.CatalogLinkPickerScreen
import app.trovata.cast.ui.screens.sessions.SellerHomeScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

object TabsHostRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<SessionsViewModel>()
        val homeState by viewModel.home.collectAsState()
        val colors = TrovataTokens.colors
        val openAccount: () -> Unit = { navigator.push(AccountRoute) }

        val clientsModel = koinScreenModel<ClientsScreenModel>()
        val clientsState by clientsModel.state.collectAsState()

        val catalogLinksModel = koinScreenModel<CatalogLinkPickerScreenModel> { parametersOf(ClientDraft()) }

        Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (homeState.activeTab) {
                    SellerTab.Sessoes -> SellerHomeScreen(
                        viewModel = viewModel,
                        onInviteClient = { navigator.push(CatalogLinkPickerScreen()) },
                        onOpenSession = { record -> navigator.push(InviteScreen(record)) },
                        onOpenAccount = openAccount,
                    )
                    SellerTab.Catalogos -> CatalogLinksTab(
                        screenModel = catalogLinksModel,
                        onOpenAccount = openAccount,
                        onSessionCreated = { record -> navigator.push(InviteScreen(record)) },
                    )
                    SellerTab.Clientes -> ClientsScreen(
                        state = clientsState,
                        onQueryChange = clientsModel::setQuery,
                        onOpenAccount = openAccount,
                        onInviteClient = { client ->
                            navigator.push(CatalogLinkPickerScreen(clientName = client.name))
                        },
                    )
                }
            }
            TabBar(active = homeState.activeTab, onSelect = viewModel::selectTab)
        }
    }
}
