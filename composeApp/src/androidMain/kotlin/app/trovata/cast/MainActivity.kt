package app.trovata.cast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.trovata.cast.platform.ActivityProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ActivityProvider.attach(this)
        setContent {
            App()
        }
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
