package app.trovata.cast.di

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.auth.AuthStore
import org.koin.dsl.module

val authModule = module {
    single { AuthRepository(get<AuthStore>(), get(), get()) }
}
