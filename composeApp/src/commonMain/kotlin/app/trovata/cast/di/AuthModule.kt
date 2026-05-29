package app.trovata.cast.di

import app.trovata.cast.data.auth.AuthRepository
import org.koin.dsl.module

val authModule = module {
    single { AuthRepository(get(), get(), get()) }
}
