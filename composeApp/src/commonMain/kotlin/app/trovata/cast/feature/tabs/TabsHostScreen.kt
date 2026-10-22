package app.trovata.cast.feature.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.feature.account.AccountRoute
import app.trovata.cast.feature.carts.OpenCartsScreenModel
import app.trovata.cast.feature.carts.OpenCartsTab
import app.trovata.cast.feature.catalog.CatalogLinkPickerRoute
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.CatalogLinksTab
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.clients.ClientsScreen
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.invite.InviteRoute
import app.trovata.cast.feature.presentation.PresentationCatalogPickerRoute
import app.trovata.cast.feature.presentation.RecordingsScreenModel
import app.trovata.cast.feature.presentation.RecordingsTab
import app.trovata.cast.feature.sessions.SellerHomeScreen
import app.trovata.cast.feature.sessions.SellerHomeViewModel
import app.trovata.cast.ui.components.SellerTab
import app.trovata.cast.ui.components.TabBar
import app.trovata.cast.ui.theme.TrovataTokens
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
        val viewModel = koinInject<SellerHomeViewModel>()
        val user by koinInject<AuthRepository>().user.collectAsState()
        val accountName = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Conta"
        val homeState by viewModel.home.collectAsState()
        val colors = TrovataTokens.colors
        val openAccount: () -> Unit = { navigator.push(AccountRoute) }

        val clientsModel = koinScreenModel<ClientsScreenModel>()
        val clientsState by clientsModel.state.collectAsState()

        val catalogLinksModel = koinScreenModel<CatalogLinkPickerScreenModel> { parametersOf(ClientDraft()) }
        val openCartsModel = koinScreenModel<OpenCartsScreenModel>()
        val recordingsModel = koinScreenModel<RecordingsScreenModel>()

        Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (homeState.activeTab) {
                    SellerTab.Sessions -> SellerHomeScreen(
                        accountName = accountName,
                        viewModel = viewModel,
                        onInviteClient = { navigator.push(CatalogLinkPickerRoute()) },
                        onOpenSession = { record -> navigator.push(InviteRoute(record)) },
                        onOpenAccount = openAccount,
                    )
                    SellerTab.Carts -> OpenCartsTab(
                        accountName = accountName,
                        screenModel = openCartsModel,
                        onOpenAccount = openAccount,
                        onSessionCreated = { record -> navigator.push(InviteRoute(record)) },
                    )
                    SellerTab.Catalogs -> CatalogLinksTab(
                        accountName = accountName,
                        screenModel = catalogLinksModel,
                        onOpenAccount = openAccount,
                        onSessionCreated = { record -> navigator.push(InviteRoute(record)) },
                    )
                    SellerTab.Recordings -> RecordingsTab(
                        accountName = accountName,
                        screenModel = recordingsModel,
                        onOpenAccount = openAccount,
                        onRecordNew = { navigator.push(PresentationCatalogPickerRoute) },
                    )
                    SellerTab.Clients -> ClientsScreen(
                        accountName = accountName,
                        state = clientsState,
                        onQueryChange = clientsModel::setQuery,
                        onOpenAccount = openAccount,
                        onInviteClient = { client ->
                            navigator.push(
                                CatalogLinkPickerRoute(
                                    clientName = client.name,
                                    clientShop = client.legalName,
                                    clientDocument = client.document,
                                    clientEmail = client.email,
                                ),
                            )
                        },
                    )
                }
            }
            TabBar(active = homeState.activeTab, onSelect = viewModel::selectTab)
        }
    }
}
