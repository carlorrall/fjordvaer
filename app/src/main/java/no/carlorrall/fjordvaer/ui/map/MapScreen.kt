package no.carlorrall.fjordvaer.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import no.carlorrall.fjordvaer.R
import no.carlorrall.fjordvaer.ui.components.FjordHeader
import no.carlorrall.fjordvaer.ui.components.LayerPills
import no.carlorrall.fjordvaer.ui.components.WeatherSheet
import no.carlorrall.fjordvaer.ui.grib.GribViewModel
import no.carlorrall.fjordvaer.ui.theme.fjordvaerColors

@Composable
fun MapScreen(viewModel: GribViewModel = viewModel(factory = GribViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = fjordvaerColors
    val mapState = rememberFjordMapState()

    // Also runs when the app comes back to the foreground, so a new hour gets a new forecast.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FjordMap(state = mapState, colors = colors, points = uiState.points)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FjordHeader()
            LayerPills(selected = uiState.selectedLayer, onSelect = viewModel::selectLayer)
        }

        // Stacked on the sheet so the button moves up when the sheet expands.
        Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.End) {
            FloatingActionButton(
                onClick = { mapState.centerOnOslofjord() },
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 12.dp)
                    .size(44.dp),
                shape = CircleShape,
                containerColor = colors.chrome,
                contentColor = colors.ink
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.locate_action))
            }

            WeatherSheet(state = uiState, onThresholdChange = viewModel::setThreshold, onRetry = viewModel::refresh)
        }
    }
}
