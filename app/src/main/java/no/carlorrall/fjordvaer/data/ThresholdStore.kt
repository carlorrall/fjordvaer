package no.carlorrall.fjordvaer.data

import android.content.Context
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.model.GribThresholds

/** Remembers the user's thresholds between launches. */
interface ThresholdStore {
    fun load(): GribThresholds
    fun save(thresholds: GribThresholds)
}

class SharedPreferencesThresholdStore(context: Context) : ThresholdStore {
    private val prefs = context.getSharedPreferences("thresholds", Context.MODE_PRIVATE)
    private val defaults = GribThresholds()

    override fun load(): GribThresholds =
        GribLayerType.entries.fold(defaults) { thresholds, layer ->
            thresholds.with(layer, prefs.getFloat(layer.name, defaults.of(layer)))
        }

    override fun save(thresholds: GribThresholds) {
        prefs.edit().apply {
            GribLayerType.entries.forEach { putFloat(it.name, thresholds.of(it)) }
        }.apply()
    }
}
