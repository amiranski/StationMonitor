package com.amiran.stationmonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.amiran.stationmonitor.data.local.entity.StationEntity
import com.amiran.stationmonitor.data.local.entity.SyncKeys
import com.amiran.stationmonitor.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Queries for the station list. Room generates the implementation (StationDao_Impl) with KSP.
 *
 * An abstract class (not an interface) so [replaceAll] can have a body and run several
 * queries inside one transaction.
 */
@Dao
abstract class StationDao {

    /** Emits the current list and again after every change of the table. */
    @Query("SELECT * FROM stations ORDER BY position")
    abstract fun observeAll(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    abstract fun observeById(stationId: String): Flow<StationEntity?>

    @Query("SELECT updatedAtEpochMillis FROM sync_state WHERE id = :key")
    abstract fun observeUpdatedAt(key: String): Flow<Long?>

    @Upsert
    abstract suspend fun upsertAll(stations: List<StationEntity>)

    @Query("DELETE FROM stations WHERE id NOT IN (:keepIds)")
    abstract suspend fun deleteStationsNotIn(keepIds: List<String>)

    @Query("DELETE FROM devices WHERE stationId NOT IN (:keepIds)")
    abstract suspend fun deleteDevicesOfStationsNotIn(keepIds: List<String>)

    @Upsert
    abstract suspend fun upsertSyncState(state: SyncStateEntity)

    /**
     * Makes the cache match the server response exactly.
     *
     * Upsert alone would never remove a station the server stopped returning, so stations that
     * are gone (and their cached devices) are deleted first. @Transaction makes the steps
     * atomic: observers never see a half-updated list.
     */
    @Transaction
    open suspend fun replaceAll(stations: List<StationEntity>, updatedAtEpochMillis: Long) {
        val ids = stations.map { it.id }
        deleteDevicesOfStationsNotIn(ids)
        deleteStationsNotIn(ids)
        upsertAll(stations)
        upsertSyncState(SyncStateEntity(SyncKeys.STATIONS, updatedAtEpochMillis))
    }
}
