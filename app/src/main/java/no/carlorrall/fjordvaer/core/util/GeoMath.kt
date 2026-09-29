package no.carlorrall.fjordvaer.core.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance in kilometres. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Speed of a vector given its u (east) and v (north) components. */
    fun speedFromUV(u: Double, v: Double): Double = sqrt(u * u + v * v)

    /**
     * Compass bearing a vector points toward, in degrees clockwise from north (0 = north,
     * 90 = east), as MapLibre's icon-rotate expects. Note the argument order: atan2(u, v).
     */
    fun bearingFromUV(u: Double, v: Double): Double {
        val bearing = Math.toDegrees(atan2(u, v))
        return (bearing + 360) % 360
    }
}

/**
 * Buckets points into cells roughly [minDistanceKm] wide so "is any earlier point too close?"
 * only has to look at the neighbouring cells.
 */
class SpatialGrid(private val minDistanceKm: Double) {
    private data class Cell(val x: Int, val y: Int)

    private val cells = mutableMapOf<Cell, MutableList<Pair<Double, Double>>>()
    private val cellSizeDegrees = minDistanceKm / 111.0

    private fun cellOf(lat: Double, lon: Double) =
        Cell((lon / cellSizeDegrees).toInt(), (lat / cellSizeDegrees).toInt())

    fun isFarFromAll(lat: Double, lon: Double): Boolean {
        val cell = cellOf(lat, lon)
        for (dx in -1..1) for (dy in -1..1) {
            cells[Cell(cell.x + dx, cell.y + dy)]?.forEach { (otherLat, otherLon) ->
                if (GeoMath.distanceKm(lat, lon, otherLat, otherLon) < minDistanceKm) return false
            }
        }
        return true
    }

    fun add(lat: Double, lon: Double) {
        cells.getOrPut(cellOf(lat, lon)) { mutableListOf() }.add(lat to lon)
    }
}
