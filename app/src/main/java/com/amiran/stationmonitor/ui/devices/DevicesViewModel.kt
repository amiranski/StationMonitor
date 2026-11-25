package com.amiran.stationmonitor.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.domain.model.sortedBySeverity
import com.amiran.stationmonitor.domain.repository.StationRepository
import com.amiran.stationmonitor.ui.common.ListUiState
import com.amiran.stationmonitor.ui.common.RefreshTracker
import com.amiran.stationmonitor.ui.common.snackbarError
import com.amiran.stationmonitor.ui.common.toListUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DevicesUiState(
    /** Station header; null only if the station is not in the cache (should not happen). */
    val station: Station? = null,
    val list: ListUiState<Device> = ListUiState.Loading,
    /** "3 Online · 1 Error" summary, in a stable order. Empty while nothing is loaded. */
    val statusCounts: List<Pair<DeviceStatus, Int>> = emptyList(),
    val snackbarError: DataError? = null,
)

/**
 * State holder of the station detail screen.
 *
 * The station id is a runtime argument (it comes from the navigation key), so it cannot be
 * provided by Hilt itself. Assisted injection: Hilt supplies the repository, the screen
 * supplies the id through [Factory].
 */
@HiltViewModel(assistedFactory = DevicesViewModel.Factory::class)
class DevicesViewModel @AssistedInject constructor(
    @Assisted private val stationId: String,
    private val repository: StationRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(stationId: String): DevicesViewModel
    }

    private val refresher = RefreshTracker(viewModelScope) { repository.refreshDevices(stationId) }

    val uiState: StateFlow<DevicesUiState> =
        combine(
            repository.observeStation(stationId),
            repository.observeDevices(stationId),
            refresher.status,
        ) { station, cachedDevices, refresh ->
            // Problems first: an operator cares about what is broken.
            val sorted = cachedDevices.copy(value = cachedDevices.value.sortedBySeverity())
            val list = sorted.toListUiState(refresh)
            DevicesUiState(
                station = station,
                list = list,
                statusCounts = statusCounts(sorted.value),
                snackbarError = list.snackbarError(refresh),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DevicesUiState(),
        )

    init {
        refresh()
    }

    fun refresh() = refresher.refresh()

    fun onErrorShown() = refresher.errorShown()
}

/** Counts per status, ordered like the device list (problems first), zero counts left out. */
internal fun statusCounts(devices: List<Device>): List<Pair<DeviceStatus, Int>> {
    val counts = devices.groupingBy { it.status }.eachCount()
    return listOf(DeviceStatus.ERROR, DeviceStatus.UNKNOWN, DeviceStatus.OFFLINE, DeviceStatus.ONLINE)
        .mapNotNull { status -> counts[status]?.let { status to it } }
}
