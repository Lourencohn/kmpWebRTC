package app.trovata.cast.di

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.sellerIdentityOf
import app.trovata.cast.data.local.AppointmentsRepository
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.PresentationsRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.local.appointmentOwnerOf
import app.trovata.cast.data.remote.PresentationsApi
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.sfa.CartApi
import app.trovata.cast.data.remote.sfa.CatalogLinksApi
import app.trovata.cast.data.remote.sfa.OpenCartsApi
import app.trovata.cast.feature.account.AccountScreenModel
import app.trovata.cast.feature.agenda.AgendaScreenModel
import app.trovata.cast.feature.agenda.AppointmentDetailScreenModel
import app.trovata.cast.feature.agenda.AppointmentFormScreenModel
import app.trovata.cast.feature.agenda.AppointmentFormSpec
import app.trovata.cast.feature.auth.LoginScreenModel
import app.trovata.cast.feature.carts.OpenCartsScreenModel
import app.trovata.cast.feature.catalog.CatalogLinkPickerScreenModel
import app.trovata.cast.feature.catalog.ClientDraft
import app.trovata.cast.feature.clients.ClientsScreenModel
import app.trovata.cast.feature.invite.InviteScreenModel
import app.trovata.cast.feature.presentation.PresentationRecorderScreenModel
import app.trovata.cast.feature.presentation.PresentationSpec
import app.trovata.cast.feature.presentation.RecordingsScreenModel
import app.trovata.cast.feature.sessions.SellerHomeViewModel
import app.trovata.cast.platform.PresentationAudioRecorder
import app.trovata.cast.protocol.buildRecorderEmbedUrl
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    factory { (record: StoredSessionRecord) ->
        InviteScreenModel(record = record, fetchSession = get<SessionsApi>()::fetchSession)
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
    factory {
        val auth = get<AuthRepository>()
        val openCarts = get<OpenCartsApi>()
        AgendaScreenModel(
            owners = combine(auth.user, auth.activeCompany, ::appointmentOwnerOf),
            observe = get<AppointmentsRepository>()::observeForOwner,
            loadOpenCarts = { slug, situacao, order, search ->
                openCarts.listForSeller(slug, situacao, order, search)
            },
        )
    }
    factory { (spec: AppointmentFormSpec) ->
        val auth = get<AuthRepository>()
        val appointments = get<AppointmentsRepository>()
        val clients = get<ClientsRepository>()
        AppointmentFormScreenModel(
            spec = spec,
            ownerProvider = { appointmentOwnerOf(auth.user.value, auth.activeCompany.value) },
            loadAppointments = appointments::listForOwner,
            searchClients = { query -> clients.search(query) },
            saveAppointment = appointments::save,
        )
    }
    factory { (appointmentId: String) ->
        val auth = get<AuthRepository>()
        val appointments = get<AppointmentsRepository>()
        val sessionsRepository = get<SessionsRepository>()
        val openCarts = get<OpenCartsApi>()
        val cartApi = get<CartApi>()
        val owner = appointmentOwnerOf(auth.user.value, auth.activeCompany.value)
        AppointmentDetailScreenModel(
            appointmentId = appointmentId,
            observeAppointments = { owner?.let(appointments::observeForOwner) ?: flowOf(emptyList()) },
            observeSessions = sessionsRepository::observeAll,
            observeOrders = get<OrderRepository>()::observeForSessions,
            findClient = get<ClientsRepository>()::findById,
            loadOpenCarts = { slug, situacao, order, search ->
                openCarts.listForSeller(slug, situacao, order, search)
            },
            loadCartItems = { slug, catalogoUuid, carrinhoId -> cartApi.items(slug, catalogoUuid, carrinhoId) },
            saveAppointment = appointments::save,
            createSession = get<SessionsApi>()::createSession,
            persistSession = sessionsRepository::persistCreated,
            sellerProvider = { sellerIdentityOf(auth.user.value, auth.activeCompany.value) },
        )
    }
}
