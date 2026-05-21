package app.trovata.cast.navigation

import androidx.compose.runtime.Composable
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.data.sample.UpcomingSession
import app.trovata.cast.ui.screens.prep.CatalogPickerScreen
import app.trovata.cast.ui.screens.sessions.IncomingCallScreen
import app.trovata.cast.ui.screens.sessions.SellerHomeScreen
import app.trovata.cast.ui.screens.sessions.SessionPrepScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object SellerHomeRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        SellerHomeScreen(
            viewModel = container.sessionsViewModel,
            onOpenIncoming = { navigator.push(IncomingCallRoute) },
            onOpenPrep = { upcoming: UpcomingSession ->
                navigator.push(SessionPrepRoute(clientName = upcoming.client.name))
            },
            onInviteClient = { navigator.push(CatalogPickerScreen()) },
        )
    }
}

object IncomingCallRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        IncomingCallScreen(
            viewModel = container.sessionsViewModel,
            onAnswered = { navigator.popUntilRoot() },
            onDeclined = { navigator.pop() },
            onRescheduled = { navigator.pop() },
        )
    }
}

data class SessionPrepRoute(val clientName: String? = null) : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        SessionPrepScreen(
            viewModel = container.sessionsViewModel,
            onBack = { navigator.pop() },
            onStartCall = {
                navigator.push(CatalogPickerScreen(clientName = clientName))
            },
        )
    }
}

