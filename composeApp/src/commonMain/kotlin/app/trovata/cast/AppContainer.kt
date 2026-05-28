package app.trovata.cast

import app.cash.sqldelight.db.SqlDriver
import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.AuthStore
import app.trovata.cast.data.local.CartRepository
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.data.remote.sfa.AccountApi
import app.trovata.cast.data.remote.sfa.KeycloakAuthService
import app.trovata.cast.data.remote.sfa.SfaApi
import app.trovata.cast.data.remote.sfa.SfaConfig
import app.trovata.cast.data.sync.CatalogSyncService
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.platform.DatabaseDriverFactory
import app.trovata.cast.platform.ShareController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(
    driverFactory: DatabaseDriverFactory,
    authStore: AuthStore,
) {
    private val driver: SqlDriver = driverFactory.create()
    private val database: TrovataDatabase = TrovataDatabase(driver)
    val httpClient = HttpClientFactory.create()
    private val sfaHttpClient = HttpClientFactory.createSfa()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sessionsRepository: SessionsRepository = SessionsRepository(database)
    val cartRepository: CartRepository = CartRepository(database)
    val orderRepository: OrderRepository = OrderRepository(database)
    val sessionsApi: SessionsApi = SessionsApi(httpClient)
    val sessionsViewModel: SessionsViewModel = SessionsViewModel(orderRepository = orderRepository)
    val shareController: ShareController = ShareController()

    private val keycloakAuth = KeycloakAuthService(sfaHttpClient)
    private val accountApi = AccountApi(sfaHttpClient)
    val authRepository: AuthRepository = AuthRepository(authStore, keycloakAuth, accountApi)

    val sfaApi: SfaApi = SfaApi(
        client = sfaHttpClient,
        tokenProvider = { authRepository.accessToken() },
        empresaIdProvider = { authRepository.empresaId() ?: SfaConfig.empresaId },
    )
    val catalogRepository: CatalogRepository = CatalogRepository(database)
    val clientsRepository: ClientsRepository = ClientsRepository(database)
    val catalogSyncService: CatalogSyncService = CatalogSyncService(sfaApi, database)

    private var syncStarted = false

    fun startCatalogSync() {
        if (syncStarted) return
        syncStarted = true
        appScope.launch { catalogSyncService.syncEssentials() }
    }
}

object AppContainerHolder {
    private var instance: AppContainer? = null

    fun init(container: AppContainer) {
        if (instance == null) instance = container
    }

    val current: AppContainer
        get() = instance ?: error("AppContainer not initialized. Call AppContainerHolder.init() first.")
}
