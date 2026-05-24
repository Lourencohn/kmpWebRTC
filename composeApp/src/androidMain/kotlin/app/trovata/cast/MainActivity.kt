package app.trovata.cast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.trovata.cast.data.auth.AuthStore
import app.trovata.cast.platform.ActivityProvider
import app.trovata.cast.platform.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppContainerHolder.init(
            AppContainer(
                driverFactory = DatabaseDriverFactory(applicationContext),
                authStore = AuthStore(applicationContext),
            ),
        )
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
