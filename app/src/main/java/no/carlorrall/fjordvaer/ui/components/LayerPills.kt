package no.carlorrall.fjordvaer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.ui.theme.fjordvaerColors

data class LayerInfo(val type: GribLayerType, val label: String, val color: Color)

@Composable
fun layerInfos(): List<LayerInfo> {
    val colors = fjordvaerColors
    return listOf(
        LayerInfo(GribLayerType.WIND, "Wind", colors.wind),
        LayerInfo(GribLayerType.CURRENT, "Current", colors.current),
        LayerInfo(GribLayerType.WAVE, "Waves", colors.wave),
        LayerInfo(GribLayerType.RAIN, "Rain", colors.rain)
    )
}

@Composable
fun LayerPills(
    selected: GribLayerType,
    onSelect: (GribLayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = fjordvaerColors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        layerInfos().forEach { info ->
            val isSelected = info.type == selected
            val background = if (isSelected) info.color else colors.chrome
            val textColor = if (isSelected) Color.White else colors.ink

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .shadow(if (isSelected) 3.dp else 2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(background)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) Color.Transparent else colors.divider,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .selectable(selected = isSelected, onClick = { onSelect(info.type) }, role = Role.RadioButton),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LayerGlyph(info.type, textColor, modifier = Modifier.size(14.dp))
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
