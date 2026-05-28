package app.trovata.cast.data.auth

import android.content.Context
import android.content.SharedPreferences

actual class AuthStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun get(key: String): String? = prefs.getString(key, null)

    actual fun put(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "trovatacast.auth"
    }
}
