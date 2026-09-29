package no.carlorrall.fjordvaer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App-specific tokens Material3's ColorScheme doesn't have names for (map tint, chrome). */
data class FjordvaerColors(
    val ink: Color,
    val muted: Color,
    val sheet: Color,
    val chrome: Color,
    val divider: Color,
    val mapLand: Color,
    val mapWater: Color,
    val mapPark: Color,
    val mapBuilding: Color,
    val mapRoad: Color,
    val mapLabel: Color,
    val mapWaterLabel: Color,
    val wind: Color = Teal,
    val current: Color = Indigo,
    val wave: Color = Blue,
    val rain: Color = Slate,
    val legendLow: Color = LegendLow,
    val legendMedium: Color = LegendMedium,
    val legendHigh: Color = LegendHigh
)

private val LightFjordvaerColors = FjordvaerColors(
    ink = LightInk, muted = LightMuted, sheet = LightSheet, chrome = LightChrome,
    divider = LightDivider, mapLand = LightMapLand, mapWater = LightMapWater, mapPark = LightMapPark,
    mapBuilding = LightMapBuilding, mapRoad = LightMapRoad, mapLabel = LightMapLabel, mapWaterLabel = LightMapWaterLabel
)

private val DarkFjordvaerColors = FjordvaerColors(
    ink = DarkInk, muted = DarkMuted, sheet = DarkSheet, chrome = DarkChrome,
    divider = DarkDivider, mapLand = DarkMapLand, mapWater = DarkMapWater, mapPark = DarkMapPark,
    mapBuilding = DarkMapBuilding, mapRoad = DarkMapRoad, mapLabel = DarkMapLabel, mapWaterLabel = DarkMapWaterLabel
)

val LocalFjordvaerColors = staticCompositionLocalOf { LightFjordvaerColors }

@Composable
fun FjordvaerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val fjordvaerColors = if (darkTheme) DarkFjordvaerColors else LightFjordvaerColors
    val materialColors = if (darkTheme) {
        darkColorScheme(primary = Teal, background = fjordvaerColors.sheet, surface = fjordvaerColors.sheet, onBackground = fjordvaerColors.ink, onSurface = fjordvaerColors.ink)
    } else {
        lightColorScheme(primary = Teal, background = fjordvaerColors.sheet, surface = fjordvaerColors.sheet, onBackground = fjordvaerColors.ink, onSurface = fjordvaerColors.ink)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalFjordvaerColors provides fjordvaerColors) {
        MaterialTheme(colorScheme = materialColors, typography = FjordvaerTypography, content = content)
    }
}

val fjordvaerColors: FjordvaerColors
    @Composable get() = LocalFjordvaerColors.current
