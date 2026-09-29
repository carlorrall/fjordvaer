package no.carlorrall.fjordvaer.ui

import no.carlorrall.fjordvaer.model.GribForecast
import no.carlorrall.fjordvaer.model.GribGrid
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.model.GribThresholds
import no.carlorrall.fjordvaer.model.IntensityLevel
import no.carlorrall.fjordvaer.model.VectorField
import no.carlorrall.fjordvaer.ui.grib.GribOverlayCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GribOverlayCalculatorTest {

    /** A 1 x 3 grid with points ~55 km apart, so spacing never removes anything. */
    private fun grid(vararg values: Float) = GribGrid(
        latitudes = floatArrayOf(59.0f),
        longitudes = floatArrayOf(10.0f, 11.0f, 12.0f),
        values = values
    )

    private val noData = GribForecast(wind = null, current = null, waveHeight = null, precipitation = null)

    @Test
    fun onlyValuesAboveTheThresholdAreShown() {
        val forecast = noData.copy(waveHeight = grid(0.1f, 0.5f, Float.NaN))
        val points = GribOverlayCalculator.calculate(forecast, GribThresholds(waveM = 0.3f), GribLayerType.WAVE)
        assertEquals(1, points.size)
        assertEquals(0.5, points.single().value, 1e-9)
    }

    @Test
    fun raisingTheThresholdRemovesPoints() {
        val forecast = noData.copy(waveHeight = grid(0.4f, 0.8f, 1.2f))
        val count = { threshold: Float ->
            GribOverlayCalculator.calculate(forecast, GribThresholds(waveM = threshold), GribLayerType.WAVE).size
        }
        assertEquals(3, count(0.2f))
        assertEquals(2, count(0.5f))
        assertEquals(0, count(1.5f))
    }

    @Test
    fun highlightLevelDependsOnHowFarAboveTheThresholdTheValueIs() {
        val forecast = noData.copy(precipitation = grid(1.2f, 1.6f, 3.0f))
        val levels = GribOverlayCalculator.calculate(forecast, GribThresholds(rainMm = 1.0f), GribLayerType.RAIN)
            .sortedBy { it.value }
            .map { it.level }
        assertEquals(listOf(IntensityLevel.LOW, IntensityLevel.MEDIUM, IntensityLevel.HIGH), levels)
    }

    @Test
    fun arrowsPointTheWayTheFlowIsGoing() {
        val u = grid(0f, 0f, 0f)
        val v = grid(5f, 5f, 5f) // pure northward component
        val forecast = noData.copy(wind = VectorField(u, v))
        val wind = GribOverlayCalculator.calculate(forecast, GribThresholds(windMs = 1f), GribLayerType.WIND).first()
        assertEquals(0f, wind.rotation!!, 1e-3f)
        assertEquals(5.0, wind.value, 1e-9)
    }

    @Test
    fun onlyTheRequestedLayerIsCalculated() {
        val u = grid(0f, 0f, 0f)
        val v = grid(5f, 5f, 5f)
        val forecast = noData.copy(wind = VectorField(u, v), waveHeight = grid(2f, 2f, 2f))
        val points = GribOverlayCalculator.calculate(forecast, GribThresholds(), GribLayerType.WAVE)
        assertTrue(points.isNotEmpty())
        assertTrue(points.all { it.type == GribLayerType.WAVE })
    }

    @Test
    fun thinningKeepsTheStrongestOfTwoNeighbours() {
        // Two cells ~1 km apart: only one fits, and it should be the stronger one.
        val forecast = noData.copy(
            waveHeight = GribGrid(floatArrayOf(59.0f), floatArrayOf(10.0f, 10.01f), floatArrayOf(0.6f, 0.9f))
        )
        val points = GribOverlayCalculator.calculate(forecast, GribThresholds(waveM = 0.3f), GribLayerType.WAVE)
        assertEquals(0.9, points.single().value, 1e-9)
    }

    @Test
    fun peakIgnoresTheThreshold() {
        val forecast = noData.copy(waveHeight = grid(0.1f, 0.7f, Float.NaN))
        assertEquals(0.7, GribOverlayCalculator.peak(forecast, GribLayerType.WAVE)!!, 1e-6)
        assertNull(GribOverlayCalculator.peak(forecast, GribLayerType.RAIN))
    }
}
