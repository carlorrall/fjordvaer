package no.carlorrall.fjordvaer

import android.app.Application
import no.carlorrall.fjordvaer.di.AppContainer
import no.carlorrall.fjordvaer.di.DefaultAppContainer
import org.maplibre.android.MapLibre

class FjordvaerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        container = DefaultAppContainer(this)
    }
}
