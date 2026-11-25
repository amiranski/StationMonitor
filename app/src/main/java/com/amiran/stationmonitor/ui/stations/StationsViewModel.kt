package com.amiran.stationmonitor.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.domain.repository.StationRepository
import com.amiran.stationmonitor.ui.common.ListUiState
import com.amiran.stationmonitor.ui.common.RefreshTracker
import com.amiran.stationmonitor.ui.common.snackbarError
import com.amiran.stationmonitor.ui.common.toListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Everything the stations screen needs to draw itself. */
data class StationsUiState(
    val list: ListUiState<Station> = ListUiState.Loading,
    /** A refresh failure to show once in a Snackbar while cached data stays visible. */
    val snackbarError: DataError? = null,
)

/**
 * State holder of the stations screen.
 *
 * Unidirectional data flow: the screen reads [uiState] and only calls [refresh] /
 * [onErrorShown]. The state is derived from the cache Flow and the refresh status; it is
 * never mutated directly.
 */
@HiltViewModel
class StationsViewModel @Inject constructor(
    private val repository: StationRepository,
) : ViewModel() {

    private val refresher = RefreshTracker(viewModelScope) { repository.refreshStations() }

    val uiState: StateFlow<StationsUiState> =
        combine(repository.observeStations(), refresher.status) { cached, refresh ->
            val list = cached.toListUiState(refresh)
            StationsUiState(list = list, snackbarError = list.snackbarError(refresh))
        }.stateIn(
            scope = viewModelScope,
            // Keeps the upstream alive 5 s after the UI stops collecting: a screen rotation
            // does not restart the database query, leaving the app for longer does.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StationsUiState(),
        )

    init {
        // Runs once per ViewModel, i.e. once per screen visit, not again on rotation.
        refresh()
    }

    fun refresh() = refresher.refresh()

    fun onErrorShown() = refresher.errorShown()
}
