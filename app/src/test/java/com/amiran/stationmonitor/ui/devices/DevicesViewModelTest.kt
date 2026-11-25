package com.amiran.stationmonitor.ui.devices

import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.RefreshResult
import com.amiran.stationmonitor.testutil.FIXED_NOW
import com.amiran.stationmonitor.testutil.FakeStationRepository
import com.amiran.stationmonitor.testutil.MainDispatcherRule
import com.amiran.stationmonitor.testutil.device
import com.amiran.stationmonitor.testutil.station
import com.amiran.stationmonitor.ui.common.ListUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeStationRepository()

    @Test
    fun `devices are shown problems first with a status summary`() = runTest {
        repository.station.value = station("s1", name = "Praha – Smíchov")
        repository.devices.value = CachedData(
            listOf(
                device("ok", DeviceStatus.ONLINE),
                device("broken", DeviceStatus.ERROR),
                device("ok2", DeviceStatus.ONLINE),
            ),
            FIXED_NOW,
        )
        val viewModel = DevicesViewModel(stationId = "s1", repository = repository)
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        repository.completeRefresh(RefreshResult.Success)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Praha – Smíchov", state.station?.name)
        assertEquals(listOf("broken", "ok", "ok2"), (state.list as ListUiState.Content).items.map { it.id })
        assertEquals(listOf(DeviceStatus.ERROR to 1, DeviceStatus.ONLINE to 2), state.statusCounts)
    }

    @Test
    fun `station without devices shows Empty after a successful load`() = runTest {
        repository.devices.value = CachedData(emptyList(), FIXED_NOW)
        val viewModel = DevicesViewModel(stationId = "s1", repository = repository)
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        repository.completeRefresh(RefreshResult.Success)
        advanceUntilIdle()

        assertEquals(ListUiState.Empty(isRefreshing = false), viewModel.uiState.value.list)
    }
}
