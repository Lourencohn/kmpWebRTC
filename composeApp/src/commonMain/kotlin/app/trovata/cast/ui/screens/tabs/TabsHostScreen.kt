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
import app.trovata.cast.navigation.IncomingCallRoute
import app.trovata.cast.navigation.SessionPrepRoute
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.SellerTab
import app.trovata.cast.ui.components.TabBar
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.screens.prep.CatalogPickerScreen
import app.trovata.cast.ui.screens.sessions.SellerHomeScreen
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
                    )
                    SellerTab.Catalogo -> EmptyTabScreen(
                        eyebrow = "Catálogo",
                        title = "Sua coleção, num só lugar",
                        subtitle = "Organize a Outono 26 e prepare o que vai mostrar.",
                        icon = TrovataIcons.grid,
                        bullets = listOf(
                            "Curadoria de drops e cápsulas por estação.",
                            "Filtros por tag (Novo, Top venda, Pré-venda).",
                            "Pré-visualização da grade do cliente antes da chamada.",
                        ),
                    )
                    SellerTab.Clientes -> EmptyTabScreen(
                        eyebrow = "Clientes",
                        title = "Quem compra com você",
                        subtitle = "Histórico, sugestões e contatos prontos para convidar.",
                        icon = TrovataIcons.users,
                        bullets = listOf(
                            "Lista de lojas com últimas compras e itens preferidos.",
                            "Sugestões de reposição a partir do histórico.",
                            "Convite direto: 1 toque e a sessão começa.",
                        ),
                    )
                    SellerTab.Insights -> EmptyTabScreen(
                        eyebrow = "Insights",
                        title = "O que está vendendo",
                        subtitle = "Métricas das sessões, top SKUs e conversão.",
                        icon = TrovataIcons.trend,
                        bullets = listOf(
                            "Top produtos vistos × pedidos por coleção.",
                            "Taxa de fechamento por cliente.",
                            "Tempo médio de sessão e itens por pedido.",
                        ),
                    )
                }
            }
            TabBar(active = homeState.activeTab, onSelect = viewModel::selectTab)
        }
    }
}
