package com.amiran.stationmonitor.ui.stations

import androidx.lifecycle.SavedStateHandle
import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.RefreshResult
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.testutil.FIXED_NOW
import com.amiran.stationmonitor.testutil.FakeStationRepository
import com.amiran.stationmonitor.testutil.MainDispatcherRule
import com.amiran.stationmonitor.testutil.station
import com.amiran.stationmonitor.ui.common.ListUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeStationRepository()

    /** uiState uses WhileSubscribed, so a test has to collect it to make it produce values. */
    private fun TestScope.createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): StationsViewModel {
        val viewModel = StationsViewModel(repository, savedStateHandle)
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `starts loading and refreshes once on creation`() = runTest {
        val viewModel = createViewModel()

        assertEquals(ListUiState.Loading, viewModel.uiState.value.list)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `successful first load shows the stations`() = runTest {
        val viewModel = createViewModel()

        repository.stations.value = CachedData(listOf(station("a")), FIXED_NOW)
        repository.completeRefresh(RefreshResult.Success)
        advanceUntilIdle()

        val list = viewModel.uiState.value.list as ListUiState.Content
        assertEquals(listOf("a"), list.items.map { it.id })
        assertEquals(false, list.isRefreshing)
        assertEquals(false, list.isStale)
    }

    @Test
    fun `first load failing without cache shows the full screen error`() = runTest {
        val viewModel = createViewModel()

        repository.completeRefresh(RefreshResult.Failure(DataError.Server(503)))
        advanceUntilIdle()

        assertEquals(ListUiState.Error(DataError.Server(503), isRetrying = false), viewModel.uiState.value.list)
        assertNull(viewModel.uiState.value.snackbarError)
    }

    @Test
    fun `failing refresh with cache keeps the data, marks it stale and asks for a Snackbar once`() = runTest {
        repository.stations.value = CachedData(listOf(station("a")), FIXED_NOW)
        val viewModel = createViewModel()

        repository.completeRefresh(RefreshResult.Failure(DataError.Timeout))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue((state.list as ListUiState.Content).isStale)
        assertEquals(DataError.Timeout, state.snackbarError)

        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.snackbarError)
    }

    @Test
    fun `refresh while a refresh is running does not start a second one`() = runTest {
        val viewModel = createViewModel()

        viewModel.refresh()
        viewModel.refresh()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `retry after an error recovers`() = runTest {
        val viewModel = createViewModel()
        repository.completeRefresh(RefreshResult.Failure(DataError.NoConnection))
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(ListUiState.Error(DataError.NoConnection, isRetrying = true), viewModel.uiState.value.list)

        repository.stations.value = CachedData(listOf(station("a")), FIXED_NOW)
        repository.completeRefresh(RefreshResult.Success)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.list is ListUiState.Content)
        assertEquals(2, repository.refreshCalls)
    }

    private val brno = Station("b", "Brno – Královo Pole", "Palackého 12, Brno", "CZ", 4)
    private val zlin = Station("z", "Zlín – Nová stanice", "třída Tomáše Bati 5, Zlín", "CZ", 0)
    private val petrzalka = Station("p", "Bratislava – Petržalka", "Panónska cesta 2", "SK", 4)

    private fun TestScope.loadedViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): StationsViewModel {
        repository.stations.value = CachedData(listOf(brno, zlin, petrzalka), FIXED_NOW)
        val viewModel = createViewModel(savedStateHandle)
        repository.completeRefresh(RefreshResult.Success)
        advanceUntilIdle()
        return viewModel
    }

    private fun StationsViewModel.visibleIds() =
        (uiState.value.list as ListUiState.Content).items.map { it.id }

    @Test
    fun `search and country filter narrow the list, counts stay for the whole cache`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onQueryChange("zlin")
        advanceUntilIdle()
        assertEquals(listOf("z"), viewModel.visibleIds())
        assertEquals("zlin", viewModel.query.value)

        viewModel.onQueryChange("")
        viewModel.onCountryToggle("SK")
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(listOf("p"), viewModel.visibleIds())
        assertEquals(3, state.totalCount)
        assertEquals(
            listOf(CountryOption("CZ", 2, isSelected = false), CountryOption("SK", 1, isSelected = true)),
            state.countries,
        )

        viewModel.onCountryToggle("SK")
        advanceUntilIdle()
        assertEquals(listOf("b", "z", "p"), viewModel.visibleIds())
    }

    @Test
    fun `filter matching nothing shows no results until the filters are cleared`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onQueryChange("praha")
        viewModel.onCountryToggle("SK")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isNoResults)

        viewModel.clearFilters()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isNoResults)
        assertFalse(viewModel.uiState.value.filter.isActive)
        assertEquals("", viewModel.query.value)
        assertEquals(3, viewModel.visibleIds().size)
    }

    @Test
    fun `filter is restored from saved state after process death`() = runTest {
        val restored = SavedStateHandle(mapOf("query" to "brno", "countries" to arrayListOf("CZ")))

        val viewModel = loadedViewModel(restored)

        assertEquals("brno", viewModel.query.value)
        assertEquals(listOf("b"), viewModel.visibleIds())
    }
}
