package no.carlorrall.fjordvaer.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.ui.grib.GribUiState
import no.carlorrall.fjordvaer.ui.theme.fjordvaerColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun unitFor(layer: GribLayerType) = when (layer) {
    GribLayerType.WIND, GribLayerType.CURRENT -> "m/s"
    GribLayerType.WAVE -> "m"
    GribLayerType.RAIN -> "mm/h"
}

/** Slider ranges sized to what the Oslofjord actually sees, so the useful part of each
 *  slider isn't squeezed into its first tenth. */
private fun maxRangeFor(layer: GribLayerType) = when (layer) {
    GribLayerType.WIND -> 20f
    GribLayerType.CURRENT -> 1.5f
    GribLayerType.WAVE -> 3f
    GribLayerType.RAIN -> 5f
}

private fun format(value: Number, layer: GribLayerType) =
    String.format(Locale.US, "%.1f %s", value.toDouble(), unitFor(layer))

private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun formatHour(millis: Long): String =
    hourFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@Composable
fun WeatherSheet(
    state: GribUiState,
    onThresholdChange: (GribLayerType, Float) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = fjordvaerColors
    val infos = layerInfos()
    val activeInfo = infos.first { it.type == state.selectedLayer }

    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = colors.sheet,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(bottom = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.muted.copy(alpha = 0.3f))
                )
            }

            if (!expanded) {
                ReadingRow(state, activeInfo, onRetry)
                Spacer(Modifier.height(18.dp))
                ThresholdRow(state, activeInfo.type, activeInfo.color, onThresholdChange)
                Spacer(Modifier.height(6.dp))
                Legend()
            } else {
                Text("All layers", style = MaterialTheme.typography.titleLarge, color = colors.ink)
                Text(
                    state.validTimeMillis?.let { "MET Norway forecast for ${formatHour(it)}" }
                        ?: "MET Norway forecast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
                infos.forEachIndexed { index, info ->
                    if (index > 0) HorizontalDivider(color = colors.divider)
                    LayerSummaryRow(state, info, onThresholdChange)
                }
                Spacer(Modifier.height(8.dp))
                Legend()
            }
        }
    }
}

@Composable
private fun ReadingRow(state: GribUiState, info: LayerInfo, onRetry: () -> Unit) {
    val colors = fjordvaerColors
    val peak = state.peaks[info.type]
    val hasData = state.peaks.isNotEmpty()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(info.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            LayerGlyph(info.type, info.color)
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                text = (info.label + " · strongest " +
                    (state.validTimeMillis?.let { "at ${formatHour(it)}" } ?: "now")).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted
            )
            Text(
                text = when {
                    peak != null -> format(peak, info.type)
                    state.isLoading -> "Loading…"
                    state.error != null -> "Unavailable"
                    else -> "No data"
                },
                style = MaterialTheme.typography.displayMedium,
                color = colors.ink
            )
        }
        if (state.error != null && !hasData) {
            TextButton(onClick = onRetry) { Text("Try again", color = info.color) }
        } else if (peak != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = state.points.size.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (state.points.isEmpty()) colors.muted else colors.ink
                )
                Text("on map", style = MaterialTheme.typography.labelSmall, color = colors.muted)
            }
        }
    }
}

@Composable
private fun ThresholdRow(
    state: GribUiState,
    layer: GribLayerType,
    accent: Color,
    onThresholdChange: (GribLayerType, Float) -> Unit
) {
    val colors = fjordvaerColors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Show spots above", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        Text(
            format(state.thresholds.of(layer), layer),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
    ThresholdSlider(layer, state.thresholds.of(layer), accent, onThresholdChange)
}

@Composable
private fun LayerSummaryRow(state: GribUiState, info: LayerInfo, onThresholdChange: (GribLayerType, Float) -> Unit) {
    val colors = fjordvaerColors
    val peak = state.peaks[info.type]
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(info.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                LayerGlyph(info.type, info.color, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(info.label, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text(
                    "Above ${format(state.thresholds.of(info.type), info.type)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            }
            Text(
                text = if (peak != null) format(peak, info.type) else "—",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink
            )
        }
        ThresholdSlider(info.type, state.thresholds.of(info.type), info.color, onThresholdChange)
    }
}

/** A slimmer slider than Material3's default: an 18dp dot on a 4dp line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdSlider(
    layer: GribLayerType,
    value: Float,
    accent: Color,
    onThresholdChange: (GribLayerType, Float) -> Unit
) {
    Slider(
        value = value,
        onValueChange = { onThresholdChange(layer, it) },
        valueRange = 0f..maxRangeFor(layer),
        thumb = {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .shadow(2.dp, CircleShape, clip = false)
                    .background(accent, CircleShape)
            )
        },
        track = { sliderState ->
            val span = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction = if (span > 0f) {
                ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
            } else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
            }
        }
    )
}

@Composable
private fun Legend() {
    val colors = fjordvaerColors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(colors.legendLow, "Just above")
        LegendDot(colors.legendMedium, "Well above")
        LegendDot(colors.legendHigh, "Far above")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = fjordvaerColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted, modifier = Modifier.padding(start = 6.dp))
    }
}
