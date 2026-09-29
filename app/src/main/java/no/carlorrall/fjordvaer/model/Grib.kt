package no.carlorrall.fjordvaer.model

/**
 * One variable from a GRIB file on a regular lat/lon grid, at a single forecast step.
 * [values] is row-major: index = latIndex * width + lonIndex.
 * [validTimeMillis] is the time that step is valid for, when the file says so.
 */
class GribGrid(
    val latitudes: FloatArray,
    val longitudes: FloatArray,
    val values: FloatArray,
    val validTimeMillis: Long? = null
) {
    val height: Int get() = latitudes.size
    val width: Int get() = longitudes.size
}

/** A vector field (wind or current) stored as its east (u) and north (v) components. */
class VectorField(val u: GribGrid, val v: GribGrid)

/** Everything the map needs from the three GRIB files (weather, current, waves). */
data class GribForecast(
    val wind: VectorField?,
    val current: VectorField?,
    val waveHeight: GribGrid?,
    val precipitation: GribGrid?,
    val validTimeMillis: Long? = null
)

enum class GribLayerType { WIND, CURRENT, WAVE, RAIN }

/** How far above the user's threshold a value is. Drives the blue/amber/red marker colour. */
enum class IntensityLevel {
    LOW, MEDIUM, HIGH;

    companion object {
        fun fromRatio(valueToThresholdRatio: Double): IntensityLevel = when {
            valueToThresholdRatio < 1.4 -> LOW
            valueToThresholdRatio < 1.9 -> MEDIUM
            else -> HIGH
        }
    }
}

/**
 * One symbol to draw on the map.
 * @param rotation compass bearing the arrow points toward, clockwise from north (wind and current only)
 * @param level marker colour, from how far above the threshold [value] is
 */
data class GribOverlayPoint(
    val type: GribLayerType,
    val latitude: Double,
    val longitude: Double,
    val value: Double,
    val level: IntensityLevel,
    val rotation: Float? = null
)

/**
 * The user's per-layer alert thresholds, in each layer's own unit. The defaults are set low
 * on purpose: Oslofjord currents rarely pass 1 m/s and waves rarely pass 1.5 m.
 */
data class GribThresholds(
    val windMs: Float = 4.0f,
    val currentMs: Float = 0.2f,
    val waveM: Float = 0.3f,
    val rainMm: Float = 0.2f
) {
    fun of(layer: GribLayerType): Float = when (layer) {
        GribLayerType.WIND -> windMs
        GribLayerType.CURRENT -> currentMs
        GribLayerType.WAVE -> waveM
        GribLayerType.RAIN -> rainMm
    }

    fun with(layer: GribLayerType, value: Float): GribThresholds = when (layer) {
        GribLayerType.WIND -> copy(windMs = value)
        GribLayerType.CURRENT -> copy(currentMs = value)
        GribLayerType.WAVE -> copy(waveM = value)
        GribLayerType.RAIN -> copy(rainMm = value)
    }
}
