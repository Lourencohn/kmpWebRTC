package app.trovata.cast.di

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.sfa.CatalogLinksApi
import app.trovata.cast.data.remote.sfa.OpenCartsApi
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.feature.carts.OpenCartsScreenModel
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.sessions.SessionsViewModel
import org.koin.dsl.module

val screenModelModule = module {
    single { SessionsViewModel(get(), get(), get()) }

    factory { LoginScreenModel(get()) }
    factory { AccountScreenModel(get(), get(), get()) }
    factory { ClientsScreenModel(get(), get()) }
    factory {
        val sessionsApi = get<SessionsApi>()
        val sessionsRepository = get<SessionsRepository>()
        val openCarts = get<OpenCartsApi>()
        val auth = get<AuthRepository>()
        OpenCartsScreenModel(
            loadOpenCarts = { slug, situacao, order, search ->
                openCarts.listForSeller(slug, situacao, order, search)
            },
            createSession = sessionsApi::createSession,
            persistSession = sessionsRepository::persistCreated,
            empresaSlugProvider = { auth.activeCompany.value?.slug.orEmpty() },
            companyNameProvider = { auth.activeCompany.value?.name.orEmpty() },
        )
    }
    factory { (initial: ClientDraft) ->
        val sessionsApi = get<SessionsApi>()
        val sessionsRepository = get<SessionsRepository>()
        val catalogLinks = get<CatalogLinksApi>()
        val auth = get<AuthRepository>()
        CatalogLinkPickerScreenModel(
            loadCatalogLinks = { slug, search -> catalogLinks.listForSeller(slug, search) },
            createSession = sessionsApi::createSession,
            persistSession = sessionsRepository::persistCreated,
            empresaSlugProvider = { auth.activeCompany.value?.slug.orEmpty() },
            companyNameProvider = { auth.activeCompany.value?.name.orEmpty() },
            initialClient = initial,
        )
    }
}
