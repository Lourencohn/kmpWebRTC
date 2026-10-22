package app.trovata.cast.platform

import android.app.Activity
import android.content.Intent
import java.lang.ref.WeakReference

object ActivityProvider {
    @Volatile
    private var ref: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        ref = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }

    fun current(): Activity? = ref?.get()
}

actual class ShareController actual constructor() {
    actual fun share(text: String, subject: String?) {
        val activity = ActivityProvider.current() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        val chooser = Intent.createChooser(intent, subject ?: "Compartilhar")
        activity.startActivity(chooser)
    }
}
