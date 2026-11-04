package app.trovata.cast.data.auth

import android.content.Context
import android.content.SharedPreferences
import app.trovata.cast.config.AppConfig

actual class AuthStore(context: Context) : AuthStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual override fun get(key: String): String? = prefs.getString(key, null)

    actual override fun put(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    private companion object {
        val PREFS_NAME = if (AppConfig.environment == "production") {
            "trovatacast.production.auth"
        } else {
            "trovatacast.auth"
        }
    }
}
