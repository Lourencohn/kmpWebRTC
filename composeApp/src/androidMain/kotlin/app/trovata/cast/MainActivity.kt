package app.trovata.cast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.trovata.cast.platform.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppContainerHolder.init(AppContainer(DatabaseDriverFactory(applicationContext)))
        setContent {
            App()
        }
    }
}
