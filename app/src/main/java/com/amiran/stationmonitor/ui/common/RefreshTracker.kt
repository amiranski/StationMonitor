package com.amiran.stationmonitor.ui.common

import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.RefreshResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the network refresh, independent of the cached data. */
data class RefreshStatus(
    val isRefreshing: Boolean = false,
    /** Error of the last finished refresh, null after a success. Drives "stale" and full-screen error. */
    val lastError: DataError? = null,
    /** Same error until the UI confirms it was shown in a Snackbar. */
    val unshownError: DataError? = null,
)

/**
 * Runs refreshes for one screen and tracks their state. Used by both ViewModels
 * (composition instead of a common base class).
 *
 * A second refresh() while one is running is ignored, so pull-to-refresh plus a Retry tap
 * never start two parallel downloads.
 */
class RefreshTracker(
    private val scope: CoroutineScope,
    private val refreshAction: suspend () -> RefreshResult,
) {
    private val _status = MutableStateFlow(RefreshStatus())
    val status: StateFlow<RefreshStatus> = _status.asStateFlow()

    private var job: Job? = null

    fun refresh() {
        if (job?.isActive == true) return
        job = scope.launch {
            _status.update { it.copy(isRefreshing = true) }
            _status.value = when (val result = refreshAction()) {
                RefreshResult.Success -> RefreshStatus()
                is RefreshResult.Failure -> RefreshStatus(
                    isRefreshing = false,
                    lastError = result.error,
                    unshownError = result.error,
                )
            }
        }
    }

    /** Called by the UI after the Snackbar was displayed, so it is not shown again (e.g. after rotation). */
    fun errorShown() {
        _status.update { it.copy(unshownError = null) }
    }
}
