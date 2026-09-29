package no.carlorrall.fjordvaer.core

import no.carlorrall.fjordvaer.core.util.GeoMath
import no.carlorrall.fjordvaer.core.util.SpatialGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun distanceBetweenOsloAndBergenIsAbout305Km() {
        val km = GeoMath.distanceKm(59.9139, 10.7522, 60.3913, 5.3221)
        assertEquals(305.0, km, 5.0)
    }

    @Test
    fun speedAndBearingFromUV() {
        assertEquals(5.0, GeoMath.speedFromUV(3.0, 4.0), 1e-9)
        assertEquals(0.0, GeoMath.bearingFromUV(0.0, 1.0), 1e-9)   // flowing north
        assertEquals(90.0, GeoMath.bearingFromUV(1.0, 0.0), 1e-9)  // flowing east
        assertEquals(180.0, GeoMath.bearingFromUV(0.0, -1.0), 1e-9) // flowing south
        assertEquals(270.0, GeoMath.bearingFromUV(-1.0, 0.0), 1e-9) // flowing west
    }

    @Test
    fun spatialGridRejectsPointsCloserThanTheMinimumDistance() {
        val grid = SpatialGrid(minDistanceKm = 5.0)
        grid.add(59.90, 10.70)
        assertFalse(grid.isFarFromAll(59.91, 10.70)) // ~1.1 km away
        assertTrue(grid.isFarFromAll(60.00, 10.70))  // ~11 km away
    }
}
