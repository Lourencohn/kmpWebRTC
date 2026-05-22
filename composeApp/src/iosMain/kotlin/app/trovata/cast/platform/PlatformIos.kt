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

actual object ServerConfig {
    actual val baseUrl: String = "http://192.168.1.101:8080"
}

actual fun httpClientEngine(): HttpClientEngine = Darwin.create()
