package app.trovata.cast

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
