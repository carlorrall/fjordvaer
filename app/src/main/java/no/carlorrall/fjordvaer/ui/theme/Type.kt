package no.carlorrall.fjordvaer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import no.carlorrall.fjordvaer.R

/**
 * Fraunces for the big readings, IBM Plex Sans for everything else. Both are fetched through
 * Android's downloadable Google Fonts provider instead of being bundled, and fall back to the
 * system font if the provider is unavailable.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun googleFont(name: String, weight: FontWeight) = Font(
    googleFont = GoogleFont(name),
    fontProvider = googleFontProvider,
    weight = weight
)

val FraunchesFamily = FontFamily(
    googleFont("Fraunces", FontWeight.Normal),
    googleFont("Fraunces", FontWeight.Medium),
    googleFont("Fraunces", FontWeight.SemiBold)
)

val PlexSansFamily = FontFamily(
    googleFont("IBM Plex Sans", FontWeight.Normal),
    googleFont("IBM Plex Sans", FontWeight.Medium),
    googleFont("IBM Plex Sans", FontWeight.SemiBold)
)

val FjordvaerTypography = Typography(
    displayMedium = TextStyle(fontFamily = FraunchesFamily, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 35.sp),
    titleLarge = TextStyle(fontFamily = FraunchesFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = PlexSansFamily, fontWeight = FontWeight.Medium, fontSize = 14.5.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = PlexSansFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = PlexSansFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = PlexSansFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = PlexSansFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = PlexSansFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
)
