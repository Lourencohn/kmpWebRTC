package app.trovata.cast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.trovata.cast.platform.ActivityProvider

class MainActivity : ComponentActivity() {
    private val callPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ActivityProvider.attach(this)
        hideSystemNavigation()
        ensureCallPermissions()
        setContent {
            App()
        }
    }

    private fun hideSystemNavigation() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    private fun ensureCallPermissions() {
        val missing = callPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        ActivityProvider.attach(this)
        hideSystemNavigation()
    }

    override fun onDestroy() {
        ActivityProvider.detach(this)
        super.onDestroy()
    }
}
