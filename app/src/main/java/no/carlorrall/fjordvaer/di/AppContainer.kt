package no.carlorrall.fjordvaer.di

import android.content.Context
import no.carlorrall.fjordvaer.core.network.HttpClientFactory
import no.carlorrall.fjordvaer.data.SharedPreferencesThresholdStore
import no.carlorrall.fjordvaer.data.ThresholdStore
import no.carlorrall.fjordvaer.data.grib.GribDataSource
import no.carlorrall.fjordvaer.data.grib.GribRepository
import no.carlorrall.fjordvaer.data.grib.GribRepositoryImpl
import java.io.File

interface AppContainer {
    val gribRepository: GribRepository
    val thresholdStore: ThresholdStore
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val gribDataSource = GribDataSource(
        client = HttpClientFactory.create(),
        directory = File(context.cacheDir, "grib")
    )

    override val gribRepository: GribRepository = GribRepositoryImpl(gribDataSource)
    override val thresholdStore: ThresholdStore = SharedPreferencesThresholdStore(context)
}
