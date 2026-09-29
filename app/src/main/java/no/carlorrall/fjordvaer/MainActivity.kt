package no.carlorrall.fjordvaer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import no.carlorrall.fjordvaer.ui.map.MapScreen
import no.carlorrall.fjordvaer.ui.theme.FjordvaerTheme
import no.carlorrall.fjordvaer.ui.theme.fjordvaerColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FjordvaerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = fjordvaerColors.sheet) {
                    MapScreen()
                }
            }
        }
    }
}
