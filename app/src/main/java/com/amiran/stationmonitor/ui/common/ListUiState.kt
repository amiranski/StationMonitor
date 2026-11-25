package com.amiran.stationmonitor.ui.common

import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.DataError
import java.time.Instant

/**
 * What a list screen shows. One sealed type = exactly one of these states at a time, so
 * impossible combinations ("loading AND error AND empty") cannot be represented.
 * Both screens (stations, devices) use it.
 */
sealed interface ListUiState<out T> {

    /** First load, nothing cached yet. */
    data object Loading : ListUiState<Nothing>

    /**
     * Cached items are shown.
     * @param isRefreshing a refresh is running (pull-to-refresh indicator).
     * @param isStale the last refresh failed, so the items may be outdated (banner).
     */
    data class Content<T>(
        val items: List<T>,
        val updatedAt: Instant?,
        val isRefreshing: Boolean,
        val isStale: Boolean,
    ) : ListUiState<T>

    /** The server returned an empty list. */
    data class Empty(val isRefreshing: Boolean) : ListUiState<Nothing>

    /** Loading failed and there is no cached data to fall back to. */
    data class Error(val error: DataError, val isRetrying: Boolean) : ListUiState<Nothing>
}

/**
 * Builds the screen state from the two sources of truth:
 * what is in the cache, and what the last refresh did.
 * A pure function, so every combination is covered by unit tests.
 */
fun <T> CachedData<List<T>>.toListUiState(refresh: RefreshStatus): ListUiState<T> {
    val neverLoaded = updatedAt == null
    return when {
        value.isNotEmpty() -> ListUiState.Content(
            items = value,
            updatedAt = updatedAt,
            isRefreshing = refresh.isRefreshing,
            isStale = refresh.lastError != null,
        )
        !neverLoaded -> ListUiState.Empty(isRefreshing = refresh.isRefreshing)
        refresh.lastError != null -> ListUiState.Error(refresh.lastError, isRetrying = refresh.isRefreshing)
        else -> ListUiState.Loading
    }
}

/**
 * Error to show in a Snackbar: only when there is something on screen to keep showing.
 * Without data the full-screen error is used instead.
 */
fun ListUiState<*>.snackbarError(refresh: RefreshStatus): DataError? = when (this) {
    is ListUiState.Content, is ListUiState.Empty -> refresh.unshownError
    ListUiState.Loading, is ListUiState.Error -> null
}
