package com.amiran.stationmonitor.ui.stations

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Everything the stations screen needs to draw itself. */
data class StationsUiState(
    /** In the Content state, `items` are already filtered by [filter]. */
    val list: ListUiState<Station> = ListUiState.Loading,
    val filter: StationFilter = StationFilter(),
    /** Country chips; empty while nothing is cached. */
    val countries: List<CountryOption> = emptyList(),
    /** Number of cached stations before filtering, for the "Showing 12 of 147" subtitle. */
    val totalCount: Int = 0,
    /** A refresh failure to show once in a Snackbar while cached data stays visible. */
    val snackbarError: DataError? = null,
) {
    /**
     * Stations are cached, but the search and filters match none of them.
     * Content is never empty without an active filter: an empty cache becomes ListUiState.Empty.
     */
    val isNoResults: Boolean
        get() = list is ListUiState.Content && list.items.isEmpty()
}

/**
 * State holder of the stations screen.
 *
 * Unidirectional data flow: the screen reads [uiState] and [query] and only calls the event
 * functions below. The state is derived from three sources (the cache, the refresh status and
 * the filter); it is never mutated directly.
 */
@HiltViewModel
class StationsViewModel @Inject constructor(
    private val repository: StationRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val refresher = RefreshTracker(viewModelScope) { repository.refreshStations() }

    /**
     * Text of the search field. SavedStateHandle keeps it across rotation and also when Android
     * kills the app in the background (process death), which a plain MutableStateFlow would not.
     * The text field reads this flow directly rather than through [uiState], so typed characters
     * reach the field without passing through the combine below.
     */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    /**
     * Stored as a sorted ArrayList: the saved state Bundle holds an ArrayList<String> natively,
     * while a Set would have to go through slower Java serialization.
     */
    private val selectedCountries: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(KEY_COUNTRIES, emptyList())

    private val filter: Flow<StationFilter> =
        combine(query, selectedCountries) { text, countries -> StationFilter(text, countries.toSet()) }

    val uiState: StateFlow<StationsUiState> =
        combine(repository.observeStations(), refresher.status, filter) { cached, refresh, currentFilter ->
            val list = cached.toListUiState(refresh)
            StationsUiState(
                list = list.filteredBy(currentFilter),
                filter = currentFilter,
                countries = cached.value.countryOptions(selected = currentFilter.countries),
                totalCount = cached.value.size,
                snackbarError = list.snackbarError(refresh),
            )
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

    fun onQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    fun onCountryToggle(code: String) {
        val current = selectedCountries.value.toSet()
        val updated = if (code in current) current - code else current + code
        savedStateHandle[KEY_COUNTRIES] = ArrayList(updated.sorted())
    }

    fun clearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_COUNTRIES] = ArrayList<String>()
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_COUNTRIES = "countries"
    }
}
