package app.trovata.cast.data.auth

import platform.Foundation.NSUserDefaults

actual class AuthStore {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual fun isAuthenticated(): Boolean = defaults.boolForKey(KEY_AUTH)

    actual fun setAuthenticated(value: Boolean) {
        defaults.setBool(value, KEY_AUTH)
    }

    private companion object {
        const val KEY_AUTH = "trovatacast.is_authenticated"
    }
}
