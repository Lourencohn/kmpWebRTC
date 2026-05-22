package app.trovata.cast.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

actual class ShareController actual constructor() {
    actual fun share(text: String, subject: String?) {
        val items: List<Any> = listOf(text)
        val activityVC = UIActivityViewController(
            activityItems = items,
            applicationActivities = null,
        )
        val presenter = topViewController() ?: return
        presenter.presentViewController(
            viewControllerToPresent = activityVC,
            animated = true,
            completion = null,
        )
    }

    private fun topViewController(): UIViewController? {
        val windows = UIApplication.sharedApplication.windows
        val keyWindow = windows
            .filterIsInstance<UIWindow>()
            .firstOrNull { it.isKeyWindow() }
            ?: windows.firstOrNull() as? UIWindow
        var top = keyWindow?.rootViewController
        while (true) {
            val presented = top?.presentedViewController
            if (presented != null) top = presented else break
        }
        return top
    }
}
