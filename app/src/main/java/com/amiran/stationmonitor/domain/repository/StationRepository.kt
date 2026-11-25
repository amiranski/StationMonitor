package com.amiran.stationmonitor.domain.repository

import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.RefreshResult
import com.amiran.stationmonitor.domain.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point to station data for the UI layer.
 *
 * Offline-first contract:
 * - `observe*` functions read ONLY the local cache and emit again whenever the cache changes.
 * - `refresh*` functions download fresh data and write it into the cache. They never return
 *   the data itself; observers receive it through the Flow.
 *
 * The interface lives in `domain`, the implementation in `data`. ViewModels depend on the
 * interface, so tests can pass a fake (dependency inversion).
 */
interface StationRepository {
    fun observeStations(): Flow<CachedData<List<Station>>>

    fun observeStation(stationId: String): Flow<Station?>

    fun observeDevices(stationId: String): Flow<CachedData<List<Device>>>

    suspend fun refreshStations(): RefreshResult

    suspend fun refreshDevices(stationId: String): RefreshResult
}
