package no.carlorrall.fjordvaer.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.carlorrall.fjordvaer.fakes.FakeGribRepository
import no.carlorrall.fjordvaer.fakes.FakeThresholdStore
import no.carlorrall.fjordvaer.model.GribForecast
import no.carlorrall.fjordvaer.model.GribGrid
import no.carlorrall.fjordvaer.model.GribLayerType
import no.carlorrall.fjordvaer.model.GribThresholds
import no.carlorrall.fjordvaer.model.VectorField
import no.carlorrall.fjordvaer.ui.grib.GribViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * uiState is a WhileSubscribed StateFlow: it only updates while something collects it, so
 * every test collects it on backgroundScope before asserting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GribViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var now = 10 * HOUR

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repository: FakeGribRepository = FakeGribRepository(),
        store: FakeThresholdStore = FakeThresholdStore()
    ) = GribViewModel(repository, store, clock = { now }, calculationDispatcher = testDispatcher)

    private fun windGrid() = GribForecast(
        wind = VectorField(
            u = GribGrid(floatArrayOf(59.0f), floatArrayOf(10.0f), floatArrayOf(0f)),
            v = GribGrid(floatArrayOf(59.0f), floatArrayOf(10.0f), floatArrayOf(8f))
        ),
        current = null, waveHeight = null, precipitation = null
    )

    @Test
    fun refreshOnlyFetchesOncePerHour() = runTest(testDispatcher) {
        val repository = FakeGribRepository().apply { result = Result.success(windGrid()) }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.refresh()
        now += 20 * MINUTE
        viewModel.refresh()
        assertEquals(1, repository.callCount)
        assertEquals(1, viewModel.uiState.value.points.size)

        now += HOUR
        viewModel.refresh()
        assertEquals(2, repository.callCount)
    }

    @Test
    fun aFailedRefreshKeepsThePreviousForecast() = runTest(testDispatcher) {
        val repository = FakeGribRepository().apply { result = Result.success(windGrid()) }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.refresh()

        repository.result = Result.failure(RuntimeException("offline"))
        now += HOUR
        viewModel.refresh()

        assertEquals(1, viewModel.uiState.value.points.size)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aFailedFirstLoadShowsAnError() = runTest(testDispatcher) {
        val repository = FakeGribRepository().apply { result = Result.failure(RuntimeException("offline")) }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.refresh()

        assertEquals("offline", viewModel.uiState.value.error)
    }

    @Test
    fun changingTheThresholdRecalculatesWithoutRefetching() = runTest(testDispatcher) {
        val repository = FakeGribRepository().apply { result = Result.success(windGrid()) }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.refresh()

        assertEquals(1, viewModel.uiState.value.points.size) // above the default wind threshold
        viewModel.setThreshold(GribLayerType.WIND, 10f) // above the 8 m/s in the fake data

        assertTrue(viewModel.uiState.value.points.isEmpty())
        assertEquals(1, repository.callCount)
    }

    @Test
    fun thresholdsSnapToOneDecimal() = runTest(testDispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setThreshold(GribLayerType.WIND, 4.0374f)

        assertEquals(4.0f, viewModel.uiState.value.thresholds.windMs, 0f)
    }

    @Test
    fun thresholdsAreLoadedAndSaved() = runTest(testDispatcher) {
        val store = FakeThresholdStore(GribThresholds(waveM = 0.9f))
        val viewModel = viewModel(store = store)
        backgroundScope.launch { viewModel.uiState.collect {} }
        assertEquals(0.9f, viewModel.uiState.value.thresholds.waveM, 0f)

        viewModel.setThreshold(GribLayerType.WAVE, 1.2f)
        viewModel.setThreshold(GribLayerType.WAVE, 1.5f)
        advanceUntilIdle()

        assertEquals(1, store.saveCount) // debounced: one save for the whole drag
        assertEquals(1.5f, store.stored.waveM, 0f)
    }

    @Test
    fun onlyTheSelectedLayerIsDrawnButPeaksCoverEveryLayer() = runTest(testDispatcher) {
        val repository = FakeGribRepository().apply { result = Result.success(windGrid()) }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.refresh()

        viewModel.selectLayer(GribLayerType.WAVE)

        assertEquals(GribLayerType.WAVE, viewModel.uiState.value.selectedLayer)
        assertTrue(viewModel.uiState.value.points.isEmpty())
        assertEquals(8.0, viewModel.uiState.value.peaks[GribLayerType.WIND]!!, 1e-9)
    }

    private companion object {
        const val MINUTE = 60 * 1000L
        const val HOUR = 60 * MINUTE
    }
}
