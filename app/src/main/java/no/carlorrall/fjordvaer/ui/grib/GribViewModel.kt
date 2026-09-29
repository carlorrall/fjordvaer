package no.carlorrall.fjordvaer.ui.grib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.carlorrall.fjordvaer.data.ThresholdStore
import no.carlorrall.fjordvaer.data.grib.GribRepository
import no.carlorrall.fjordvaer.model.GribForecast
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.model.GribOverlayPoint
import no.carlorrall.fjordvaer.model.GribThresholds
import no.carlorrall.fjordvaer.ui.appContainer
import kotlin.math.roundToInt

private const val HOUR_MILLIS = 60 * 60 * 1000L
private const val SAVE_DEBOUNCE_MILLIS = 400L

data class GribUiState(
    val selectedLayer: GribLayerType = GribLayerType.WIND,
    val thresholds: GribThresholds = GribThresholds(),
    /** Symbols to draw for [selectedLayer] only. */
    val points: List<GribOverlayPoint> = emptyList(),
    /** Highest value per layer across the whole fjord, independent of the threshold. */
    val peaks: Map<GribLayerType, Double?> = emptyMap(),
    /** The hour the forecast shown is valid for. */
    val validTimeMillis: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Loads the GRIB forecast and recalculates the map symbols whenever the user changes a
 * threshold or layer. Changing a slider does not download anything — only
 * GribOverlayCalculator reruns.
 */
@OptIn(FlowPreview::class)
class GribViewModel(
    private val repository: GribRepository,
    private val thresholdStore: ThresholdStore,
    private val clock: () -> Long = System::currentTimeMillis,
    // Injectable so tests can use a test dispatcher the scheduler can wait for.
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private data class LoadState(
        val forecast: GribForecast? = null,
        val loadedAt: Long = 0L,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val loadState = MutableStateFlow(LoadState())
    private val selectedLayer = MutableStateFlow(GribLayerType.WIND)
    private val thresholds = MutableStateFlow(thresholdStore.load())

    val uiState: StateFlow<GribUiState> = combine(loadState, selectedLayer, thresholds) { state, layer, currentThresholds ->
        GribUiState(
            selectedLayer = layer,
            thresholds = currentThresholds,
            points = state.forecast?.let { GribOverlayCalculator.calculate(it, currentThresholds, layer) }.orEmpty(),
            peaks = state.forecast?.let { forecast ->
                GribLayerType.entries.associateWith { GribOverlayCalculator.peak(forecast, it) }
            }.orEmpty(),
            validTimeMillis = state.forecast?.validTimeMillis,
            isLoading = state.isLoading,
            error = state.error
        )
    }
        .flowOn(calculationDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GribUiState(thresholds = thresholds.value))

    init {
        viewModelScope.launch {
            thresholds.drop(1).debounce(SAVE_DEBOUNCE_MILLIS).collect(thresholdStore::save)
        }
    }

    /**
     * Loads the forecast if there is none yet, or if the one shown is from an earlier hour.
     * Safe to call on every resume. A failed refresh keeps showing the previous forecast.
     */
    fun refresh() {
        val current = loadState.value
        if (current.isLoading) return
        if (current.forecast != null && current.loadedAt / HOUR_MILLIS == clock() / HOUR_MILLIS) return
        loadState.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.getForecast()
                .onSuccess { loadState.value = LoadState(forecast = it, loadedAt = clock()) }
                .onFailure {
                    loadState.value = current.copy(
                        isLoading = false,
                        error = if (current.forecast == null) it.message ?: "Could not load weather data" else null
                    )
                }
        }
    }

    fun selectLayer(layer: GribLayerType) {
        selectedLayer.value = layer
    }

    /** Snaps to 0.1 so the value shown is the value used, and the map only redraws per step. */
    fun setThreshold(layer: GribLayerType, value: Float) {
        thresholds.value = thresholds.value.with(layer, (value * 10).roundToInt() / 10f)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { GribViewModel(appContainer.gribRepository, appContainer.thresholdStore) }
        }
    }
}
