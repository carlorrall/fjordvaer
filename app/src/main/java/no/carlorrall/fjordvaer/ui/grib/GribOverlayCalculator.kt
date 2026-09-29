package no.carlorrall.fjordvaer.ui.grib

import no.carlorrall.fjordvaer.core.util.GeoMath
import no.carlorrall.fjordvaer.core.util.SpatialGrid
import no.carlorrall.fjordvaer.model.GribForecast
import no.carlorrall.fjordvaer.model.GribGrid
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.model.GribOverlayPoint
import no.carlorrall.fjordvaer.model.GribThresholds
import no.carlorrall.fjordvaer.model.IntensityLevel
import no.carlorrall.fjordvaer.model.VectorField
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Turns raw GRIB grids into the symbols drawn on the map.
 *
 * 1. Only grid points of the selected layer above the user's threshold are kept.
 * 2. Points are thinned so symbols do not overlap, strongest first — so thinning drops the
 *    calmer neighbours of a peak, never the peak itself.
 */
object GribOverlayCalculator {

    private const val MIN_DISTANCE_BETWEEN_SYMBOLS_KM = 8.0

    fun calculate(forecast: GribForecast, thresholds: GribThresholds, layer: GribLayerType): List<GribOverlayPoint> {
        val threshold = thresholds.of(layer)
        val candidates = when (layer) {
            GribLayerType.WIND -> forecast.wind?.let { vectorCandidates(it, layer, threshold) }
            GribLayerType.CURRENT -> forecast.current?.let { vectorCandidates(it, layer, threshold) }
            GribLayerType.WAVE -> forecast.waveHeight?.let { scalarCandidates(it, layer, threshold) }
            GribLayerType.RAIN -> forecast.precipitation?.let { scalarCandidates(it, layer, threshold) }
        }.orEmpty()

        val placed = SpatialGrid(MIN_DISTANCE_BETWEEN_SYMBOLS_KM)
        return candidates.sortedByDescending { it.value }.filter { point ->
            placed.isFarFromAll(point.latitude, point.longitude).also { free ->
                if (free) placed.add(point.latitude, point.longitude)
            }
        }
    }

    /** The highest value anywhere in the layer, regardless of threshold — null if there is no data. */
    fun peak(forecast: GribForecast, layer: GribLayerType): Double? = when (layer) {
        GribLayerType.WIND -> forecast.wind?.let(::vectorPeak)
        GribLayerType.CURRENT -> forecast.current?.let(::vectorPeak)
        GribLayerType.WAVE -> forecast.waveHeight?.let(::scalarPeak)
        GribLayerType.RAIN -> forecast.precipitation?.let(::scalarPeak)
    }

    private fun vectorCandidates(field: VectorField, type: GribLayerType, threshold: Float): List<GribOverlayPoint> =
        cells(field.u).mapNotNull { (index, lat, lon) ->
            val u = field.u.values[index].toDouble()
            val v = field.v.values.getOrNull(index)?.toDouble() ?: Double.NaN
            val speed = GeoMath.speedFromUV(u, v)
            if (speed.isNaN() || speed <= threshold) return@mapNotNull null
            GribOverlayPoint(
                type, lat, lon, round2(speed),
                level = levelFor(speed, threshold),
                rotation = GeoMath.bearingFromUV(u, v).toFloat()
            )
        }.toList()

    private fun scalarCandidates(grid: GribGrid, type: GribLayerType, threshold: Float): List<GribOverlayPoint> =
        cells(grid).mapNotNull { (index, lat, lon) ->
            val value = grid.values[index].toDouble()
            if (value.isNaN() || value <= threshold) return@mapNotNull null
            GribOverlayPoint(type, lat, lon, round2(value), level = levelFor(value, threshold))
        }.toList()

    private fun vectorPeak(field: VectorField): Double? =
        field.u.values.indices.mapNotNull { i ->
            val v = field.v.values.getOrNull(i) ?: return@mapNotNull null
            GeoMath.speedFromUV(field.u.values[i].toDouble(), v.toDouble()).takeIf { !it.isNaN() }
        }.maxOrNull()

    private fun scalarPeak(grid: GribGrid): Double? =
        grid.values.filter { !it.isNaN() }.maxOrNull()?.toDouble()

    /** Every (index, lat, lon) cell of the grid that has a value slot. */
    private fun cells(grid: GribGrid): Sequence<Triple<Int, Double, Double>> = sequence {
        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val index = y * grid.width + x
                if (index >= grid.values.size) continue
                yield(Triple(index, grid.latitudes[y].toDouble(), grid.longitudes[x].toDouble()))
            }
        }
    }

    private fun levelFor(value: Double, threshold: Float): IntensityLevel =
        IntensityLevel.fromRatio(if (threshold > 0f) value / threshold else Double.MAX_VALUE)

    private fun round2(value: Double): Double =
        BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
