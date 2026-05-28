package app.trovata.cast.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.ui.screens.account.AccountScreen
import app.trovata.cast.ui.screens.auth.AuthLoginScreen
import app.trovata.cast.ui.screens.auth.CompanySelectionScreen
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.ui.screens.auth.AuthWelcomeScreen
import app.trovata.cast.ui.screens.catalog.ProductDetailScreen
import app.trovata.cast.ui.screens.prep.CatalogPickerScreen
import app.trovata.cast.ui.screens.sessions.IncomingCallScreen
import app.trovata.cast.ui.screens.sessions.SessionPrepScreen
import cafe.adriel.voyager.core.model.rememberScreenModel
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

data class ProductDetailRoute(val productRef: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val product = SampleCatalog.products.firstOrNull { it.ref == productRef }
            ?: return run { navigator.pop() }
        ProductDetailScreen(
            product = product,
            onBack = { navigator.pop() },
            onStartSession = { navigator.push(CatalogPickerScreen()) },
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
            onSignOut = { container.authRepository.logout() },
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
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { LoginScreenModel(container.authRepository) }
        val state by model.state.collectAsState()

        LaunchedEffect(state.success) {
            val success = state.success ?: return@LaunchedEffect
            if (success.needsCompanySelection) navigator.push(CompanySelectionRoute)
        }

        AuthLoginScreen(
            state = state,
            onBack = { navigator.pop() },
            onEmailChange = model::setEmail,
            onPasswordChange = model::setPassword,
            onSubmit = model::submit,
        )
    }
}

object CompanySelectionRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val companies by container.authRepository.companies.collectAsState()
        CompanySelectionScreen(
            companies = companies,
            onBack = { navigator.pop() },
            onSelect = { container.authRepository.selectCompany(it.id) },
        )
    }
}

