package app.trovata.cast.di

import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.feature.catalog.CatalogPickerScreenModel
import app.trovata.cast.feature.catalog.CatalogScreenModel
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.dashboard.DashboardScreenModel
import app.trovata.cast.feature.insights.InsightsScreenModel
import app.trovata.cast.feature.sessions.SessionsViewModel
import org.koin.dsl.module

val screenModelModule = module {
    single { SessionsViewModel(get(), get(), get()) }

    factory { LoginScreenModel(get()) }
    factory { AccountScreenModel(get(), get(), get()) }
    factory { CatalogScreenModel(get(), get(), get()) }
    factory { ClientsScreenModel(get(), get()) }
    factory { InsightsScreenModel(get(), get(), get(), get()) }
    factory { DashboardScreenModel(get(), get(), get(), get(), get()) }
    factory { (initial: ClientDraft) ->
        val sessionsApi = get<SessionsApi>()
        val sessionsRepository = get<SessionsRepository>()
        val catalog = get<CatalogRepository>()
        CatalogPickerScreenModel(
            createSession = sessionsApi::createSession,
            persistSession = sessionsRepository::persistCreated,
            initialClient = initial,
            countProducts = catalog::count,
            loadPage = { limit, offset -> catalog.uiPage(limit, offset) },
        )
    }
}
