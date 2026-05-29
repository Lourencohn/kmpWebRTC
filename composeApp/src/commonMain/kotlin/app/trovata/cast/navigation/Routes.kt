package app.trovata.cast.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.trovata.cast.AppContainerHolder
import app.trovata.cast.data.sample.Product
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.ui.screens.account.AccountScreen
import app.trovata.cast.ui.screens.auth.AuthLoginScreen
import app.trovata.cast.ui.screens.auth.CompanySelectionScreen
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
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val product by produceState<Product?>(initialValue = null, productRef) {
            value = container.catalogRepository.uiProductByRef(productRef)
        }
        val images by produceState(initialValue = emptyList<String>(), productRef) {
            value = container.catalogRepository.gallery(productRef)
        }
        val related by produceState(initialValue = emptyList<Product>(), productRef) {
            value = container.catalogRepository.uiPage(8, 0).filter { it.ref != productRef }.take(6)
        }
        product?.let {
            ProductDetailScreen(
                product = it,
                imageUrls = images,
                related = related,
                onBack = { navigator.pop() },
                onStartSession = { navigator.push(CatalogPickerScreen()) },
            )
        }
    }
}

object AccountRoute : Screen {
    @Composable
    override fun Content() {
        val container = AppContainerHolder.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel {
            AccountScreenModel(
                authRepository = container.authRepository,
                orderRepository = container.orderRepository,
                clientsRepository = container.clientsRepository,
            )
        }
        val state by model.state.collectAsState()
        AccountScreen(
            state = state,
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

