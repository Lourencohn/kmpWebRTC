package app.trovata.cast.di

import app.trovata.cast.data.sync.ClientSyncCoordinator
import app.trovata.cast.data.sync.ClientSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val AppScope = named("appScope")

val syncModule = module {
    single<CoroutineScope>(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { ClientSyncService(get(), get()) }
    single { ClientSyncCoordinator(get(), get(AppScope)) }
}
