package app.trovata.cast.data.auth

import android.content.Context
import android.content.SharedPreferences

actual class AuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun isAuthenticated(): Boolean = prefs.getBoolean(KEY_AUTH, false)

    actual fun setAuthenticated(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH, value).apply()
    }

    private companion object {
        const val PREFS_NAME = "trovatacast.auth"
        const val KEY_AUTH = "is_authenticated"
    }
}
