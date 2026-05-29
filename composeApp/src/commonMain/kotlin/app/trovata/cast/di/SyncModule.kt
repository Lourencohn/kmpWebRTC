package app.trovata.cast.di

import app.trovata.cast.data.sync.CatalogSyncCoordinator
import app.trovata.cast.data.sync.CatalogSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val AppScope = named("appScope")

val syncModule = module {
    single<CoroutineScope>(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { CatalogSyncService(get(), get()) }
    single { CatalogSyncCoordinator(get(), get(AppScope)) }
}
