package app.trovata.cast.navigation

import androidx.compose.runtime.Composable
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.ui.screens.account.AccountScreen
import app.trovata.cast.ui.screens.auth.AuthLoginScreen
import app.trovata.cast.ui.screens.auth.AuthSetupScreen
import app.trovata.cast.ui.screens.auth.AuthVerifyScreen
import app.trovata.cast.ui.screens.auth.AuthWelcomeScreen
import app.trovata.cast.ui.screens.prep.CatalogPickerScreen
import app.trovata.cast.ui.screens.sessions.IncomingCallScreen
import app.trovata.cast.ui.screens.sessions.SessionPrepScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

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

object AccountRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        AccountScreen(
            onBack = { navigator.pop() },
            onSignOut = { container.authRepository.signOut() },
        )
    }
}

object AuthWelcomeRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AuthWelcomeScreen(
            onEnterWithWhatsapp = { navigator.push(AuthLoginRoute) },
            onExistingAccount = { navigator.push(AuthLoginRoute) },
        )
    }
}

object AuthLoginRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AuthLoginScreen(
            onBack = { navigator.pop() },
            onContinue = { navigator.push(AuthVerifyRoute) },
            onUseEmail = { navigator.push(AuthVerifyRoute) },
            onCreateAccount = { navigator.push(AuthVerifyRoute) },
        )
    }
}

object AuthVerifyRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AuthVerifyScreen(
            onBack = { navigator.pop() },
            onContinue = { navigator.push(AuthSetupRoute) },
            onChangeNumber = { navigator.pop() },
        )
    }
}

object AuthSetupRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        AuthSetupScreen(
            onBack = { navigator.pop() },
            onFinish = { container.authRepository.signIn() },
        )
    }
}

