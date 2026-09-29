package no.carlorrall.fjordvaer.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import no.carlorrall.fjordvaer.ui.theme.fjordvaerColors

@Composable
fun FjordHeader(modifier: Modifier = Modifier) {
    val colors = fjordvaerColors
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.chrome,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fjordvær", style = MaterialTheme.typography.titleLarge, color = colors.ink)
            Spacer(Modifier.weight(1f))
            Text("Oslofjord · MET Norway", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        }
    }
}
