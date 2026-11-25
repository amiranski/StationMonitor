package com.amiran.stationmonitor.testutil

import com.amiran.stationmonitor.data.local.dao.DeviceDao
import com.amiran.stationmonitor.data.local.dao.StationDao
import com.amiran.stationmonitor.data.local.entity.DeviceEntity
import com.amiran.stationmonitor.data.local.entity.StationEntity
import com.amiran.stationmonitor.data.local.entity.SyncStateEntity
import com.amiran.stationmonitor.data.remote.StationApi
import com.amiran.stationmonitor.data.remote.dto.DeviceDto
import com.amiran.stationmonitor.data.remote.dto.StationDto
import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.RefreshResult
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.domain.repository.StationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/*
 * Hand-written fakes instead of a mocking library: simple in-memory implementations of the
 * same interfaces / abstract classes the production code depends on.
 */

/** In-memory "database" shared by the fake DAOs, like the real tables are shared. */
class FakeTables {
    val stations = MutableStateFlow<List<StationEntity>>(emptyList())
    val devices = MutableStateFlow<List<DeviceEntity>>(emptyList())
    val syncState = MutableStateFlow<Map<String, Long>>(emptyMap())
}

/**
 * Extends the real abstract DAO. Only the primitive queries are faked; replaceAll() is the
 * production implementation, so the tests also cover its logic.
 */
class FakeStationDao(private val tables: FakeTables) : StationDao() {
    override fun observeAll(): Flow<List<StationEntity>> = tables.stations.map { list -> list.sortedBy { it.position } }

    override fun observeById(stationId: String): Flow<StationEntity?> =
        tables.stations.map { list -> list.firstOrNull { it.id == stationId } }

    override fun observeUpdatedAt(key: String): Flow<Long?> = tables.syncState.map { it[key] }

    override suspend fun upsertAll(stations: List<StationEntity>) {
        tables.stations.update { current -> (current.associateBy { it.id } + stations.associateBy { it.id }).values.toList() }
    }

    override suspend fun deleteStationsNotIn(keepIds: List<String>) {
        tables.stations.update { current -> current.filter { it.id in keepIds } }
    }

    override suspend fun deleteDevicesOfStationsNotIn(keepIds: List<String>) {
        tables.devices.update { current -> current.filter { it.stationId in keepIds } }
    }

    override suspend fun upsertSyncState(state: SyncStateEntity) {
        tables.syncState.update { it + (state.id to state.updatedAtEpochMillis) }
    }
}

class FakeDeviceDao(private val tables: FakeTables) : DeviceDao() {
    override fun observeForStation(stationId: String): Flow<List<DeviceEntity>> =
        tables.devices.map { list -> list.filter { it.stationId == stationId }.sortedBy { it.position } }

    override fun observeUpdatedAt(key: String): Flow<Long?> = tables.syncState.map { it[key] }

    override suspend fun deleteForStation(stationId: String) {
        tables.devices.update { current -> current.filterNot { it.stationId == stationId } }
    }

    override suspend fun insertAll(devices: List<DeviceEntity>) {
        tables.devices.update { current -> current + devices }
    }

    override suspend fun upsertSyncState(state: SyncStateEntity) {
        tables.syncState.update { it + (state.id to state.updatedAtEpochMillis) }
    }
}

/** API whose responses are scripted per call: a value or an exception. */
class FakeStationApi : StationApi {
    val stationResponses = ArrayDeque<Result<List<StationDto>>>()
    val deviceResponses = ArrayDeque<Result<List<DeviceDto>>>()
    var stationCalls = 0
        private set

    override suspend fun getStations(): List<StationDto> {
        stationCalls++
        return stationResponses.removeFirst().getOrThrow()
    }

    override suspend fun getDevices(stationId: String): List<DeviceDto> =
        deviceResponses.removeFirst().getOrThrow()
}

/**
 * Repository fake for ViewModel tests. The cache is a MutableStateFlow the test can change;
 * every refresh suspends until the test calls [completeRefresh], so intermediate states
 * ("refreshing") can be observed.
 */
class FakeStationRepository : StationRepository {
    val stations = MutableStateFlow(CachedData<List<Station>>(emptyList(), updatedAt = null))
    val devices = MutableStateFlow(CachedData<List<Device>>(emptyList(), updatedAt = null))
    val station = MutableStateFlow<Station?>(null)

    private var pendingRefresh = CompletableDeferred<RefreshResult>()
    var refreshCalls = 0
        private set

    fun completeRefresh(result: RefreshResult) {
        pendingRefresh.complete(result)
    }

    private suspend fun awaitRefresh(): RefreshResult {
        refreshCalls++
        val result = pendingRefresh.await()
        pendingRefresh = CompletableDeferred()
        return result
    }

    override fun observeStations(): Flow<CachedData<List<Station>>> = stations
    override fun observeStation(stationId: String): Flow<Station?> = station
    override fun observeDevices(stationId: String): Flow<CachedData<List<Device>>> = devices
    override suspend fun refreshStations(): RefreshResult = awaitRefresh()
    override suspend fun refreshDevices(stationId: String): RefreshResult = awaitRefresh()
}
