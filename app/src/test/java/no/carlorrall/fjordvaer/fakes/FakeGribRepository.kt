package no.carlorrall.fjordvaer.fakes

import no.carlorrall.fjordvaer.data.grib.GribRepository
import no.carlorrall.fjordvaer.model.GribForecast

class FakeGribRepository : GribRepository {
    var result: Result<GribForecast>? = null
    var callCount = 0

    override suspend fun getForecast(): Result<GribForecast> {
        callCount++
        return result ?: Result.success(GribForecast(wind = null, current = null, waveHeight = null, precipitation = null))
    }
}
