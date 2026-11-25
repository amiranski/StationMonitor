package com.amiran.stationmonitor.data.repository

import com.amiran.stationmonitor.data.local.dao.DeviceDao
import com.amiran.stationmonitor.data.local.dao.StationDao
import com.amiran.stationmonitor.data.local.entity.SyncKeys
import com.amiran.stationmonitor.data.mapper.toDomain
import com.amiran.stationmonitor.data.mapper.toEntity
import com.amiran.stationmonitor.data.remote.RetryPolicy
import com.amiran.stationmonitor.data.remote.StationApi
import com.amiran.stationmonitor.data.remote.toDataError
import com.amiran.stationmonitor.data.remote.withRetry
import com.amiran.stationmonitor.domain.model.CachedData
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.RefreshResult
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.domain.repository.StationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [StationRepository].
 *
 * Reading: always from Room. Room Flows emit again after every write, so the UI updates on its
 * own when a refresh stores new data.
 * Writing: refresh = download (with retries) -> map to entities -> replace the cache in one
 * transaction. On failure the cache is left untouched and the error is returned.
 *
 * Retrofit and Room suspend functions are main-safe (they switch to background threads
 * internally), so no withContext(Dispatchers.IO) is needed here.
 */
@Singleton
class OfflineFirstStationRepository @Inject constructor(
    private val api: StationApi,
    private val stationDao: StationDao,
    private val deviceDao: DeviceDao,
    private val retryPolicy: RetryPolicy,
    private val clock: Clock,
) : StationRepository {

    override fun observeStations(): Flow<CachedData<List<Station>>> =
        combine(
            stationDao.observeAll(),
            stationDao.observeUpdatedAt(SyncKeys.STATIONS),
        ) { entities, updatedAt ->
            CachedData(
                value = entities.map { it.toDomain() },
                updatedAt = updatedAt?.let(Instant::ofEpochMilli),
            )
        }

    override fun observeStation(stationId: String): Flow<Station?> =
        stationDao.observeById(stationId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override fun observeDevices(stationId: String): Flow<CachedData<List<Device>>> =
        combine(
            deviceDao.observeForStation(stationId),
            deviceDao.observeUpdatedAt(SyncKeys.devices(stationId)),
        ) { entities, updatedAt ->
            CachedData(
                value = entities.map { it.toDomain() },
                updatedAt = updatedAt?.let(Instant::ofEpochMilli),
            )
        }

    override suspend fun refreshStations(): RefreshResult = runRefresh {
        val stations = withRetry(retryPolicy) { api.getStations() }
        stationDao.replaceAll(
            stations = stations.mapIndexed { index, dto -> dto.toEntity(position = index) },
            updatedAtEpochMillis = clock.millis(),
        )
    }

    override suspend fun refreshDevices(stationId: String): RefreshResult = runRefresh {
        val devices = withRetry(retryPolicy) { api.getDevices(stationId) }
        deviceDao.replaceForStation(
            stationId = stationId,
            devices = devices.mapIndexed { index, dto -> dto.toEntity(stationId, position = index) },
            updatedAtEpochMillis = clock.millis(),
        )
    }

    /**
     * Turns exceptions into [RefreshResult.Failure]. CancellationException is rethrown: it is
     * not an error but the signal that the caller (e.g. a closed screen) stopped waiting.
     */
    private suspend fun runRefresh(block: suspend () -> Unit): RefreshResult = try {
        block()
        RefreshResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        RefreshResult.Failure(e.toDataError())
    }
}
