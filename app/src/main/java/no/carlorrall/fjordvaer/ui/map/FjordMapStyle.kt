package no.carlorrall.fjordvaer.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import no.carlorrall.fjordvaer.ui.theme.FjordvaerColors
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property.NONE
import org.maplibre.android.style.layers.PropertyFactory.backgroundColor
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.maps.Style

/** OpenFreeMap's Positron style: free vector tiles, no API key needed. */
const val BASEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

/**
 * Recolors the stock OpenFreeMap style to match the app's palette. Layer ids vary a little
 * between OpenFreeMap releases, so this matches on keywords in the id / source-layer instead
 * of exact ids, and a layer that rejects a property is skipped rather than crashing.
 *
 * Positron has no land polygon: land is the background layer, with water drawn on top.
 */
object FjordMapStyle {

    private val waterPattern = Regex("water|ocean|sea|lake|river")
    private val greenPattern = Regex("park|wood|forest|grass|landcover")
    private val transportPattern = Regex("transportation|highway|road|railway|aeroway|pier|tunnel|bridge")

    private class Palette(colors: FjordvaerColors) {
        val land = colors.mapLand.toHex()
        val water = colors.mapWater.toHex()
        val park = colors.mapPark.toHex()
        val building = colors.mapBuilding.toHex()
        val road = colors.mapRoad.toHex()
        val label = colors.mapLabel.toHex()
        val waterLabel = colors.mapWaterLabel.toHex()
    }

    fun apply(style: Style, colors: FjordvaerColors) {
        val palette = Palette(colors)
        style.layers.forEach { layer ->
            runCatching { recolor(layer, palette) }
        }
    }

    private fun recolor(layer: Layer, p: Palette) {
        val tag = (layer.id + " " + sourceLayerOf(layer)).lowercase()
        when (layer) {
            is BackgroundLayer -> layer.setProperties(backgroundColor(p.land))
            is FillLayer -> when {
                waterPattern.containsMatchIn(tag) -> layer.setProperties(fillColor(p.water), fillOpacity(1f))
                "building" in tag -> layer.setProperties(fillColor(p.building), fillOpacity(0.8f))
                greenPattern.containsMatchIn(tag) -> layer.setProperties(fillColor(p.park), fillOpacity(1f))
                transportPattern.containsMatchIn(tag) -> layer.setProperties(fillColor(p.road))
                // Residential areas and anything else: blend into the land.
                else -> layer.setProperties(fillColor(p.land))
            }
            is LineLayer -> when {
                Regex("boundary|admin").containsMatchIn(tag) -> layer.setProperties(visibility(NONE))
                waterPattern.containsMatchIn(tag) -> layer.setProperties(lineColor(p.water))
                // Casings and inner lines get the same colour: roads read as one soft line
                // instead of competing with the markers.
                transportPattern.containsMatchIn(tag) -> layer.setProperties(lineColor(p.road))
            }
            is SymbolLayer -> when {
                // The weather markers live in the same style; this runs again on theme changes.
                GribMarkers.ownsLayer(layer.id) -> Unit
                // Road names, shields and airport labels are noise on a weather map. Hide them
                // by name rather than hiding everything unmatched, which would catch place labels.
                Regex("transportation_name|highway|road_shield|aerodrome|airport").containsMatchIn(tag) ->
                    layer.setProperties(visibility(NONE))
                Regex("water_name|waterway").containsMatchIn(tag) ->
                    layer.setProperties(textColor(p.waterLabel), textHaloColor(p.water))
                Regex("place|label").containsMatchIn(tag) ->
                    layer.setProperties(textColor(p.label), textHaloColor(p.land))
                else -> Unit
            }
        }
    }

    private fun sourceLayerOf(layer: Layer): String = when (layer) {
        is FillLayer -> layer.sourceLayer ?: ""
        is LineLayer -> layer.sourceLayer ?: ""
        is SymbolLayer -> layer.sourceLayer ?: ""
        else -> ""
    }

    private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
}
