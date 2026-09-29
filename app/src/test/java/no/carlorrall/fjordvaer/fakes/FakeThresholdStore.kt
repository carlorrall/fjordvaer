package no.carlorrall.fjordvaer.fakes

import no.carlorrall.fjordvaer.data.ThresholdStore
import no.carlorrall.fjordvaer.model.GribThresholds

class FakeThresholdStore(var stored: GribThresholds = GribThresholds()) : ThresholdStore {
    var saveCount = 0

    override fun load(): GribThresholds = stored

    override fun save(thresholds: GribThresholds) {
        saveCount++
        stored = thresholds
    }
}
