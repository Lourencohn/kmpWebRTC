package app.trovata.cast

import androidx.compose.ui.window.ComposeUIViewController
import app.trovata.cast.platform.DatabaseDriverFactory
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    AppContainerHolder.init(AppContainer(DatabaseDriverFactory()))
    return ComposeUIViewController { App() }
}
