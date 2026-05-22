package app.trovata.cast.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.trovata.cast.db.TrovataDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(TrovataDatabase.Schema, context, "trovatacast.db")
}

actual object ServerConfig {
    actual val baseUrl: String = "https://trovatacast-signaling.fly.dev"
}

actual fun httpClientEngine(): HttpClientEngine = OkHttp.create()
