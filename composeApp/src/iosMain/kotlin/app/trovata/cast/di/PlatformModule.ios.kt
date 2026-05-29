package app.trovata.cast.di

import app.trovata.cast.data.auth.AuthStore
import app.trovata.cast.platform.DatabaseDriverFactory
import app.trovata.cast.platform.ShareController
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single { AuthStore() }
    single { ShareController() }
}
