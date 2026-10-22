package app.trovata.cast.di

import app.cash.sqldelight.db.SqlDriver
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.platform.DatabaseDriverFactory
import org.koin.dsl.module

val databaseModule = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().create() }
    single { TrovataDatabase(get()) }
}
