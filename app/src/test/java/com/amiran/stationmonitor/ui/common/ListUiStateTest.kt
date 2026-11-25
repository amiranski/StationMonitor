package com.amiran.stationmonitor.ui.common

import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.testutil.FIXED_NOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Every combination of "what is cached" x "what the refresh did". */
class ListUiStateTest {

    private val neverLoaded = CachedData(emptyList<String>(), updatedAt = null)
    private val loadedEmpty = CachedData(emptyList<String>(), updatedAt = FIXED_NOW)
    private val cached = CachedData(listOf("a", "b"), updatedAt = FIXED_NOW)

    private val idle = RefreshStatus()
    private val refreshing = RefreshStatus(isRefreshing = true)
    private val failed = RefreshStatus(lastError = DataError.Timeout, unshownError = DataError.Timeout)

    @Test
    fun `no cache and first refresh running - Loading`() {
        assertEquals(ListUiState.Loading, neverLoaded.toListUiState(refreshing))
        assertEquals(ListUiState.Loading, neverLoaded.toListUiState(idle))
    }

    @Test
    fun `no cache and refresh failed - full screen Error`() {
        assertEquals(ListUiState.Error(DataError.Timeout, isRetrying = false), neverLoaded.toListUiState(failed))
    }

    @Test
    fun `no cache, retry running after a failure - Error with spinner`() {
        val retrying = failed.copy(isRefreshing = true)
        assertEquals(ListUiState.Error(DataError.Timeout, isRetrying = true), neverLoaded.toListUiState(retrying))
    }

    @Test
    fun `server returned an empty list - Empty`() {
        assertEquals(ListUiState.Empty(isRefreshing = false), loadedEmpty.toListUiState(idle))
    }

    @Test
    fun `cached data with refresh running - Content with indicator`() {
        val state = cached.toListUiState(refreshing)
        assertEquals(ListUiState.Content(listOf("a", "b"), FIXED_NOW, isRefreshing = true, isStale = false), state)
    }

    @Test
    fun `cached data and refresh failed - stale Content plus Snackbar`() {
        val state = cached.toListUiState(failed)

        assertEquals(ListUiState.Content(listOf("a", "b"), FIXED_NOW, isRefreshing = false, isStale = true), state)
        assertEquals(DataError.Timeout, state.snackbarError(failed))
    }

    @Test
    fun `full screen error does not also show a Snackbar`() {
        assertNull(neverLoaded.toListUiState(failed).snackbarError(failed))
    }

    @Test
    fun `Snackbar disappears once shown`() {
        val shown = failed.copy(unshownError = null)
        assertNull(cached.toListUiState(shown).snackbarError(shown))
    }
}
