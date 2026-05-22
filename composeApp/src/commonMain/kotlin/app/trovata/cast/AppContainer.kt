package app.trovata.cast

import app.cash.sqldelight.db.SqlDriver
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.SessionsApi
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.feature.sessions.SessionsViewModel
import app.trovata.cast.platform.DatabaseDriverFactory
import app.trovata.cast.platform.ShareController

class AppContainer(driverFactory: DatabaseDriverFactory) {
    private val driver: SqlDriver = driverFactory.create()
    private val database: TrovataDatabase = TrovataDatabase(driver)
    private val httpClient = HttpClientFactory.create()

    val sessionsRepository: SessionsRepository = SessionsRepository(database)
    val sessionsApi: SessionsApi = SessionsApi(httpClient)
    val sessionsViewModel: SessionsViewModel = SessionsViewModel()
    val shareController: ShareController = ShareController()
}

object AppContainerHolder {
    private var instance: AppContainer? = null

    fun init(container: AppContainer) {
        if (instance == null) instance = container
    }

    val current: AppContainer
        get() = instance ?: error("AppContainer not initialized. Call AppContainerHolder.init() first.")
}
