package app.trovata.cast.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.sample.Product
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.ui.screens.account.AccountScreen
import app.trovata.cast.ui.screens.auth.AuthLoginScreen
import app.trovata.cast.ui.screens.auth.CompanySelectionScreen
import app.trovata.cast.ui.screens.auth.AuthWelcomeScreen
import app.trovata.cast.ui.screens.catalog.ProductDetailScreen
import app.trovata.cast.ui.screens.prep.CatalogLinkPickerScreen
import app.trovata.cast.ui.screens.sessions.IncomingCallScreen
import app.trovata.cast.ui.screens.sessions.SessionPrepScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

object IncomingCallRoute : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinInject<SessionsViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        IncomingCallScreen(
            viewModel = viewModel,
            onAnswered = { navigator.popUntilRoot() },
            onDeclined = { navigator.pop() },
            onRescheduled = { navigator.pop() },
        )
    }
}

data class SessionPrepRoute(val clientName: String? = null) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinInject<SessionsViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        SessionPrepScreen(
            viewModel = viewModel,
            onBack = { navigator.pop() },
            onStartCall = {
                navigator.push(CatalogLinkPickerScreen(clientName = clientName))
            },
        )
    }
}

data class ProductDetailRoute(val productRef: String) : Screen {
    @Composable
    override fun Content() {
        val catalogRepository = koinInject<CatalogRepository>()
        val navigator = LocalNavigator.currentOrThrow
        val product by produceState<Product?>(initialValue = null, productRef) {
            value = catalogRepository.uiProductByRef(productRef)
        }
        val images by produceState(initialValue = emptyList<String>(), productRef) {
            value = catalogRepository.gallery(productRef)
        }
        val related by produceState(initialValue = emptyList<Product>(), productRef) {
            value = catalogRepository.uiPage(8, 0).filter { it.ref != productRef }.take(6)
        }
        product?.let {
            ProductDetailScreen(
                product = it,
                imageUrls = images,
                related = related,
                onBack = { navigator.pop() },
                onStartSession = { navigator.push(CatalogLinkPickerScreen()) },
            )
        }
    }
}

object AccountRoute : Screen {
    @Composable
    override fun Content() {
        val authRepository = koinInject<AuthRepository>()
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<AccountScreenModel>()
        val state by model.state.collectAsState()
        AccountScreen(
            state = state,
            onBack = { navigator.pop() },
            onSignOut = { authRepository.logout() },
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
        val model = koinScreenModel<LoginScreenModel>()
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
        val authRepository = koinInject<AuthRepository>()
        val navigator = LocalNavigator.currentOrThrow
        val companies by authRepository.companies.collectAsState()
        CompanySelectionScreen(
            companies = companies,
            onBack = { navigator.pop() },
            onSelect = { authRepository.selectCompany(it.id) },
        )
    }
}
