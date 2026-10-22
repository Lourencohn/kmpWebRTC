package app.trovata.cast.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.trovata.cast.db.TrovataDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(TrovataDatabase.Schema, "trovatacast.db")
}

actual fun httpClientEngine(): HttpClientEngine = Darwin.create()
