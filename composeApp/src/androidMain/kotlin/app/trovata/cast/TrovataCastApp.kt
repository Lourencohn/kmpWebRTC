package app.trovata.cast

import android.app.Application
import app.trovata.cast.di.initKoin
import org.koin.android.ext.koin.androidContext

class TrovataCastApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TrovataCastApp)
        }
    }
}
