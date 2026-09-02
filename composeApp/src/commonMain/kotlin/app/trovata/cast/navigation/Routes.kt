package app.trovata.cast.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.ui.screens.account.AccountScreen
import app.trovata.cast.ui.screens.auth.AuthLoginScreen
import app.trovata.cast.ui.screens.auth.CompanySelectionScreen
import app.trovata.cast.ui.screens.auth.AuthWelcomeScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.compose.koinInject

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
        AuthWelcomeScreen(onEnter = { navigator.push(AuthLoginRoute) })
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
