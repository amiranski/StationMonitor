package com.amiran.stationmonitor.ui.stations

import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.RefreshResult
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
    private fun TestScope.createViewModel(): StationsViewModel {
        val viewModel = StationsViewModel(repository)
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
}
