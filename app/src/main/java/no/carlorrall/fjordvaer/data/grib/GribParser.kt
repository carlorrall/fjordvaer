package no.carlorrall.fjordvaer.data.grib

import android.util.Log
import no.carlorrall.fjordvaer.model.GribGrid
import ucar.ma2.Array as NcArray
import ucar.ma2.Index
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import java.io.File
import java.time.Instant

private const val TAG = "GribParser"

/**
 * Reads variables from a GRIB file with NetCDF-Java. The file is opened once and every
 * requested variable is read from it.
 */
object GribParser {

    /**
     * Returns a grid per variable name that exists in the file, at the forecast step covering
     * [nowMillis]. Missing variables are skipped.
     */
    fun parse(file: File, variableNames: List<String>, nowMillis: Long): Map<String, GribGrid> =
        NetcdfDatasets.openDataset(file.absolutePath).use { dataset ->
            val latVar = dataset.findVariable("lat") ?: dataset.findVariable("latitude")
                ?: run {
                    Log.w(TAG, "${file.name}: no lat/latitude variable — actual names: ${dataset.variables.map { it.shortName }}")
                    return emptyMap()
                }
            val lonVar = dataset.findVariable("lon") ?: dataset.findVariable("longitude")
                ?: run {
                    Log.w(TAG, "${file.name}: no lon/longitude variable — actual names: ${dataset.variables.map { it.shortName }}")
                    return emptyMap()
                }
            val latitudes = readAxis(latVar)
            val longitudes = readAxis(lonVar)

            val missing = variableNames.filter { dataset.findVariable(it) == null }
            if (missing.isNotEmpty()) {
                Log.w(
                    TAG,
                    "${file.name}: requested variable(s) not found: $missing — actual names in file: " +
                        dataset.variables.map { it.shortName }
                )
            }

            variableNames.mapNotNull { name ->
                val variable = dataset.findVariable(name) ?: return@mapNotNull null
                val step = currentStep(dataset, variable, nowMillis)
                val values = readGrid(variable, step.index, latitudes.size, longitudes.size)
                name to GribGrid(latitudes, longitudes, values, step.validTimeMillis)
            }.toMap()
        }

    private fun readAxis(variable: Variable): FloatArray {
        val data = variable.read()
        return FloatArray(data.size.toInt()) { i -> data.getFloat(i) }
    }

    private class Step(val index: Int, val validTimeMillis: Long?)

    /**
     * Picks the first forecast step at or after [nowMillis]. Step 0 is when the model ran,
     * which can be many hours ago. Rounding up also suits precipitation, which is stored per
     * hour ending at each step: the next step is the hour we are in.
     */
    private fun currentStep(dataset: NetcdfDataset, variable: Variable, nowMillis: Long): Step {
        val timeDim = variable.dimensions.firstOrNull() ?: return Step(0, null)
        val timeVar = dataset.findVariable(timeDim.shortName) ?: return Step(0, null)
        val (unitMillis, reference) = parseTimeUnits(timeVar.unitsString) ?: run {
            Log.w(TAG, "Unrecognised time units '${timeVar.unitsString}', using the first step")
            return Step(0, null)
        }
        val steps = timeVar.read()
        val times = LongArray(steps.size.toInt()) { i -> reference + (steps.getDouble(i) * unitMillis).toLong() }
        if (times.isEmpty()) return Step(0, null)
        val index = times.indexOfFirst { it >= nowMillis }.takeIf { it >= 0 } ?: times.lastIndex
        return Step(index, times[index])
    }

    /** Parses CF-style units like "Hour since 2026-09-29T06:00:00Z" into (millis per unit, epoch millis). */
    internal fun parseTimeUnits(units: String?): Pair<Long, Long>? {
        val match = Regex("""^\s*(\w+)\s+since\s+(.+?)\s*$""", RegexOption.IGNORE_CASE).find(units ?: return null)
            ?: return null
        val unitMillis = when (match.groupValues[1].lowercase().removeSuffix("s")) {
            "day" -> 86_400_000L
            "hour" -> 3_600_000L
            "minute" -> 60_000L
            "second" -> 1_000L
            else -> return null
        }
        val raw = match.groupValues[2].replace(' ', 'T')
        val reference = runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching { Instant.parse(raw + "Z") }.getOrNull()
            ?: return null
        return unitMillis to reference.toEpochMilli()
    }

    /**
     * Reads one time step (and the first level) of a (time, [level,] lat, lon) variable.
     * Values that cannot be read become NaN so they are filtered out later.
     */
    private fun readGrid(variable: Variable, timeIndex: Int, height: Int, width: Int): FloatArray {
        val data: NcArray = variable.read()
        val shape = data.shape
        val rank = shape.size
        val values = FloatArray(width * height) { Float.NaN }
        if (rank < 2) return values

        val index: Index = data.index
        for (dim in 0 until rank - 2) index.setDim(dim, 0)
        if (rank > 2) index.setDim(0, timeIndex.coerceIn(0, shape[0] - 1))
        val rows = minOf(height, shape[rank - 2])
        val cols = minOf(width, shape[rank - 1])
        for (y in 0 until rows) {
            index.setDim(rank - 2, y)
            for (x in 0 until cols) {
                index.setDim(rank - 1, x)
                values[y * width + x] = data.getDouble(index).toFloat()
            }
        }
        return values
    }
}
