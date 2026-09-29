package no.carlorrall.fjordvaer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import no.carlorrall.fjordvaer.model.GribLayerType

/** Line icons for the four layers, drawn in one shared stroke style. */
@Composable
fun LayerGlyph(type: GribLayerType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (type) {
            GribLayerType.WIND -> {
                drawPath(windPath(w, h, yFraction = 0.34f), color, style = stroke)
                drawPath(windPath(w, h, yFraction = 0.62f), color, style = stroke)
            }
            GribLayerType.CURRENT -> {
                drawPath(wavePath(w, h, yFraction = 0.38f), color, style = stroke)
                drawPath(wavePath(w, h, yFraction = 0.66f), color, style = stroke)
            }
            GribLayerType.WAVE -> drawPath(wavePath(w, h, yFraction = 0.5f, bold = true), color, style = stroke)
            GribLayerType.RAIN -> drawRain(this, w, h, color, stroke)
        }
    }
}

private fun windPath(w: Float, h: Float, yFraction: Float): Path = Path().apply {
    val y = h * yFraction
    moveTo(w * 0.1f, y)
    lineTo(w * 0.62f, y)
    cubicTo(w * 0.62f, y - h * 0.2f, w * 0.9f, y - h * 0.2f, w * 0.9f, y - h * 0.05f)
}

private fun wavePath(w: Float, h: Float, yFraction: Float, bold: Boolean = false): Path = Path().apply {
    val y = h * yFraction
    val amplitude = h * (if (bold) 0.18f else 0.14f)
    moveTo(w * 0.06f, y)
    cubicTo(w * 0.25f, y - amplitude, w * 0.35f, y + amplitude, w * 0.5f, y)
    cubicTo(w * 0.65f, y - amplitude, w * 0.75f, y + amplitude, w * 0.94f, y)
}

private fun drawRain(scope: androidx.compose.ui.graphics.drawscope.DrawScope, w: Float, h: Float, color: Color, stroke: Stroke) {
    val cloud = Path().apply {
        moveTo(w * 0.28f, h * 0.55f)
        cubicTo(w * 0.05f, h * 0.55f, w * 0.05f, h * 0.28f, w * 0.28f, h * 0.3f)
        cubicTo(w * 0.32f, h * 0.12f, w * 0.68f, h * 0.14f, w * 0.7f, h * 0.32f)
        cubicTo(w * 0.95f, h * 0.3f, w * 0.95f, h * 0.55f, w * 0.75f, h * 0.55f)
        close()
    }
    scope.drawPath(cloud, color, style = stroke)
    listOf(0.36f, 0.52f, 0.68f).forEachIndexed { index, x ->
        val yOffset = if (index == 1) h * 0.86f else h * 0.78f
        scope.drawLine(color, Offset(w * x, h * 0.66f), Offset(w * x, yOffset), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
