package app.trovata.cast.di

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.sellerIdentityOf
import app.trovata.cast.data.local.PresentationsRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.remote.PresentationsApi
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.sfa.CatalogLinksApi
import app.trovata.cast.data.remote.sfa.OpenCartsApi
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.feature.carts.OpenCartsScreenModel
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.presentation.PresentationRecorderScreenModel
import app.trovata.cast.feature.presentation.PresentationSpec
import app.trovata.cast.feature.presentation.RecordingsScreenModel
import app.trovata.cast.feature.sessions.SellerHomeViewModel
import app.trovata.cast.platform.PresentationAudioRecorder
import app.trovata.cast.protocol.buildRecorderEmbedUrl
import org.koin.dsl.module

val screenModelModule = module {
    single { SellerHomeViewModel(get(), get(), get()) }

    factory { LoginScreenModel(get()) }
    factory { AccountScreenModel(get(), get(), get()) }
    factory { ClientsScreenModel(get(), get()) }
    factory {
        val presentations = get<PresentationsRepository>()
        RecordingsScreenModel(
            companies = get<AuthRepository>().activeCompany,
            observe = presentations::observeForCompany,
            removePresentation = presentations::remove,
        )
    }
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
            sellerProvider = { sellerIdentityOf(auth.user.value, auth.activeCompany.value) },
        )
    }
    factory { (catalogoUuid: String, catalogoNome: String) ->
        val api = get<PresentationsApi>()
        val presentations = get<PresentationsRepository>()
        val auth = get<AuthRepository>()
        val company = auth.activeCompany.value
        val seller = sellerIdentityOf(auth.user.value, company)
        val spec = PresentationSpec(
            empresaSlug = company?.slug.orEmpty(),
            catalogoUuid = catalogoUuid,
            catalogoNome = catalogoNome,
            sellerId = seller.id,
            sellerName = seller.name,
        )
        PresentationRecorderScreenModel(
            spec = spec,
            pageUrl = buildRecorderEmbedUrl(
                catalogBaseUrl = AppConfig.catalogWebUrlOverride ?: AppConfig.catalogWebUrl,
                empresaSlug = spec.empresaSlug,
                catalogoUuid = spec.catalogoUuid,
            ),
            audio = get<PresentationAudioRecorder>(),
            createPresentation = api::create,
            upload = api::upload,
            persist = presentations::save,
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
            sellerProvider = { sellerIdentityOf(auth.user.value, auth.activeCompany.value) },
            initialClient = initial,
        )
    }
}
