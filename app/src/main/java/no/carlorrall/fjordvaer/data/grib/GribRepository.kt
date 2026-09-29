package no.carlorrall.fjordvaer.data.grib

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import no.carlorrall.fjordvaer.model.GribForecast
import no.carlorrall.fjordvaer.model.GribGrid
import no.carlorrall.fjordvaer.model.VectorField

private const val TAG = "GribRepository"
private const val HOUR_MILLIS = 60 * 60 * 1000L

interface GribRepository {
    /** Wind, current, waves and precipitation for the Oslofjord, for the current hour. */
    suspend fun getForecast(): Result<GribForecast>
}

/**
 * Keeps the parsed forecast in memory until the clock hour changes. The forecast is stored
 * per hour, so a new hour means picking a new time step — even when the files on disk are
 * still fresh enough that [GribDataSource] won't download them again.
 */
class GribRepositoryImpl(
    private val dataSource: GribDataSource,
    private val clock: () -> Long = System::currentTimeMillis
) : GribRepository {

    private val mutex = Mutex()
    private var cached: GribForecast? = null
    private var cachedAt = 0L

    override suspend fun getForecast(): Result<GribForecast> = mutex.withLock {
        cached?.takeIf { clock() / HOUR_MILLIS == cachedAt / HOUR_MILLIS }?.let { return Result.success(it) }
        try {
            val forecast = load()
            cached = forecast
            cachedAt = clock()
            Result.success(forecast)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Downloads the three files in parallel, then reads the variables we need from each. */
    private suspend fun load(): GribForecast = coroutineScope {
        val weather = async { readOrEmpty(GribContent.WEATHER, listOf(WIND_U, WIND_V, PRECIPITATION)) }
        val current = async { readOrEmpty(GribContent.CURRENT, listOf(CURRENT_U, CURRENT_V)) }
        val waves = async { readOrEmpty(GribContent.WAVES, listOf(WAVE_HEIGHT)) }

        val weatherGrids = weather.await()
        val currentGrids = current.await()
        val waveGrids = waves.await()
        val allGrids = weatherGrids.values + currentGrids.values + waveGrids.values
        if (allGrids.isEmpty()) throw IllegalStateException("Could not fetch weather data")

        GribForecast(
            wind = vectorField(weatherGrids[WIND_U], weatherGrids[WIND_V]),
            current = vectorField(currentGrids[CURRENT_U], currentGrids[CURRENT_V]),
            waveHeight = waveGrids[WAVE_HEIGHT],
            precipitation = weatherGrids[PRECIPITATION],
            validTimeMillis = allGrids.mapNotNull { it.validTimeMillis }.maxOrNull()
        )
    }

    /** One failing product (e.g. waves) should not hide the others, so errors are logged and skipped. */
    private suspend fun readOrEmpty(content: GribContent, variables: List<String>): Map<String, GribGrid> =
        try {
            val file = dataSource.getFile(content)
            withContext(Dispatchers.Default) { GribParser.parse(file, variables, clock()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "${content.apiName}: failed to fetch or parse", e)
            emptyMap()
        }

    private fun vectorField(u: GribGrid?, v: GribGrid?): VectorField? =
        if (u != null && v != null) VectorField(u, v) else null

    private companion object {
        const val WIND_U = "u-component_of_wind_height_above_ground"
        const val WIND_V = "v-component_of_wind_height_above_ground"
        const val PRECIPITATION = "Total_precipitation_height_above_ground"
        const val CURRENT_U = "u-component_of_current_depth_below_sea"
        const val CURRENT_V = "v-component_of_current_depth_below_sea"
        const val WAVE_HEIGHT = "Significant_height_of_combined_wind_waves_and_swell_height_above_ground"
    }
}
