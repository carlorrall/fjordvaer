package no.carlorrall.fjordvaer.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import no.carlorrall.fjordvaer.model.GribOverlayPoint
import no.carlorrall.fjordvaer.model.IntensityLevel
import no.carlorrall.fjordvaer.ui.theme.LegendHigh
import no.carlorrall.fjordvaer.ui.theme.LegendLow
import no.carlorrall.fjordvaer.ui.theme.LegendMedium
import org.json.JSONObject
import org.maplibre.android.style.expressions.Expression.coalesce
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import java.util.Locale

/**
 * Draws the GRIB overlay for the selected layer: wind and current as arrows pointing the way
 * the flow is going, waves and rain as dots — all coloured by the same light/moderate/strong
 * scale as the legend, with the value underneath. Which points to draw is decided by
 * GribOverlayCalculator.
 */
object GribMarkers {

    private const val SOURCE_ID = "grib-source"
    private const val LAYER_ID = "grib-markers"

    /** Must be a font the basemap's glyph server hosts. MapLibre's default (Open Sans) isn't
     *  on OpenFreeMap, and a missing font silently drops every label. */
    private val LABEL_FONT = arrayOf("Noto Sans Bold")

    private const val ICON_DP = 26f

    fun ownsLayer(id: String): Boolean = id == LAYER_ID

    /** @param density screen density, so the icons are drawn at their real pixel size. */
    fun setup(style: Style, density: Float) {
        IntensityLevel.entries.forEach { level ->
            style.addImage(arrowId(level), arrowBitmap(level.color(), density))
            style.addImage(dotId(level), dotBitmap(level.color(), density))
        }

        style.addSource(GeoJsonSource(SOURCE_ID))
        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.iconImage(get("icon")),
                PropertyFactory.iconRotate(coalesce(get("rotation"), literal(0f))),
                // Bearings are geographic, so the arrows turn with the map when it's rotated.
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.textField(get("label")),
                PropertyFactory.textFont(LABEL_FONT),
                PropertyFactory.textSize(11f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                PropertyFactory.textOffset(arrayOf(0f, 1.25f)),
                PropertyFactory.textColor("#243230"),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(1.4f),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )
    }

    fun update(style: Style, points: List<GribOverlayPoint>) {
        style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(featureCollection(points))
    }

    private fun featureCollection(points: List<GribOverlayPoint>): String {
        val features = points.joinToString(",") { point ->
            val props = JSONObject().apply {
                put("label", String.format(Locale.US, "%.1f", point.value))
                if (point.rotation != null) {
                    put("icon", arrowId(point.level))
                    put("rotation", point.rotation.toDouble())
                } else {
                    put("icon", dotId(point.level))
                }
            }
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]},"properties":$props}"""
        }
        return """{"type":"FeatureCollection","features":[$features]}"""
    }

    private fun arrowId(level: IntensityLevel) = "grib-arrow-${level.name.lowercase()}"
    private fun dotId(level: IntensityLevel) = "grib-dot-${level.name.lowercase()}"

    private fun IntensityLevel.color(): Int = when (this) {
        IntensityLevel.LOW -> LegendLow
        IntensityLevel.MEDIUM -> LegendMedium
        IntensityLevel.HIGH -> LegendHigh
    }.toArgb()

    /** A coloured disc with a white ring and a white arrow pointing up (north at rotation 0). */
    private fun arrowBitmap(color: Int, density: Float): Bitmap {
        val size = (ICON_DP * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawDisc(canvas, size, color, density)
        val arrow = Path().apply {
            moveTo(size * 0.5f, size * 0.24f)
            lineTo(size * 0.7f, size * 0.7f)
            lineTo(size * 0.5f, size * 0.58f)
            lineTo(size * 0.3f, size * 0.7f)
            close()
        }
        canvas.drawPath(arrow, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.WHITE })
        return bitmap
    }

    private fun dotBitmap(color: Int, density: Float): Bitmap {
        val size = (ICON_DP * 0.7f * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawDisc(Canvas(bitmap), size, color, density)
        return bitmap
    }

    private fun drawDisc(canvas: Canvas, size: Int, color: Int, density: Float) {
        val center = size / 2f
        val ring = 2f * density
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.argb(40, 0, 0, 0) }
        canvas.drawCircle(center, center, center, shadow)
        canvas.drawCircle(center, center, center - density * 0.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.WHITE })
        canvas.drawCircle(center, center, center - ring, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }
}
