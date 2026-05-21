package app.trovata.cast.platform

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.engine.HttpClientEngine

expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

expect object ServerConfig {
    val baseUrl: String
}

expect fun httpClientEngine(): HttpClientEngine
