package app.trovata.cast.platform

expect class ShareController() {
    fun share(text: String, subject: String? = null)
}
