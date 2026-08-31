package app.trovata.cast.di

import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.SessionsRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { SessionsRepository(get()) }
    single { OrderRepository(get()) }
    single { CatalogRepository(get()) }
    single { ClientsRepository(get()) }
}
