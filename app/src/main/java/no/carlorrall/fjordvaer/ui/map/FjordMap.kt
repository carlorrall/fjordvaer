package no.carlorrall.fjordvaer.ui.map

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import no.carlorrall.fjordvaer.model.GribOverlayPoint
import no.carlorrall.fjordvaer.ui.theme.FjordvaerColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Centre of MET's Oslofjord GRIB area (58.9–60.0°N, 9.8–11.2°E), framed so the whole area
 *  is on screen — the outer fjord is often where the only values above a threshold are. */
val OslofjordCenter = LatLng(59.3, 10.5)
private const val DEFAULT_ZOOM = 7.95

/** Holds the live map + style once MapLibre has finished loading, so the screen can push
 *  GRIB points and theme changes into it without the map view itself living in the UI state. */
class FjordMapState {
    var map: MapLibreMap? = null
        internal set
    var style: Style? = null
        internal set

    fun centerOnOslofjord() {
        map?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(OslofjordCenter).zoom(DEFAULT_ZOOM).build()
            )
        )
    }
}

@Composable
fun rememberFjordMapState(): FjordMapState = remember { FjordMapState() }

/**
 * A MapLibre map wired to the Compose/Android lifecycle, recolored to the app's palette on
 * load and again whenever [colors] changes (the light/dark toggle).
 */
@Composable
fun FjordMap(
    state: FjordMapState,
    colors: FjordvaerColors,
    points: List<GribOverlayPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentColors = rememberUpdatedState(colors)
    var styleVersion by remember { mutableIntStateOf(0) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { map ->
                state.map = map
                map.cameraPosition = CameraPosition.Builder().target(OslofjordCenter).zoom(DEFAULT_ZOOM).build()
                map.setStyle(Style.Builder().fromUri(BASEMAP_STYLE_URL)) { style ->
                    FjordMapStyle.apply(style, currentColors.value)
                    GribMarkers.setup(style, context.resources.displayMetrics.density)
                    state.style = style
                    styleVersion++
                }
            }
        }
    }

    // MapView needs the standard Android lifecycle calls forwarded to it, or its GL surface
    // leaks and it keeps rendering (and downloading tiles) while the screen is backgrounded.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(modifier = modifier.fillMaxSize(), factory = { mapView })

    LaunchedEffect(colors, styleVersion) {
        state.style?.let { FjordMapStyle.apply(it, colors) }
    }

    LaunchedEffect(points, styleVersion) {
        state.style?.let { GribMarkers.update(it, points) }
    }
}
