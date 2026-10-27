package app.trovata.cast.di

import app.trovata.cast.data.local.AppointmentsRepository
import app.trovata.cast.data.local.CallInterestRecorder
import app.trovata.cast.data.local.CallInterestRepository
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.PresentationsRepository
import app.trovata.cast.data.local.SessionsRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { SessionsRepository(get()) }
    single { OrderRepository(get()) }
    single { ClientsRepository(get()) }
    single { PresentationsRepository(get()) }
    single { AppointmentsRepository(get()) }
    single { CallInterestRepository(get()) }
    single { CallInterestRecorder(get(), get(AppScope)) }
}
