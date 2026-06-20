package app.trovata.cast

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.trovata.cast.platform.ActivityProvider

class MainActivity : ComponentActivity() {
    private val callPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ActivityProvider.attach(this)
        ensureCallPermissions()
        setContent {
            App()
        }
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
    }

    override fun onDestroy() {
        ActivityProvider.detach(this)
        super.onDestroy()
    }
}
