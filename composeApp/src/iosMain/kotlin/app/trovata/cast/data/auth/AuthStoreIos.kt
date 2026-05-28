package app.trovata.cast.data.auth

import platform.Foundation.NSUserDefaults

actual class AuthStore {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual fun get(key: String): String? = defaults.stringForKey(key)

    actual fun put(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, key)
        }
    }
}
