package app.trovata.cast

import androidx.compose.ui.window.ComposeUIViewController
import app.trovata.cast.di.initKoin
import platform.UIKit.UIViewController

private var koinStarted = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        koinStarted = true
        initKoin()
    }
    return ComposeUIViewController { App() }
}
