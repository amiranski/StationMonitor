package com.amiran.stationmonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.amiran.stationmonitor.data.local.entity.DeviceEntity
import com.amiran.stationmonitor.data.local.entity.SyncKeys
import com.amiran.stationmonitor.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

/** Queries for the devices of one station. */
@Dao
abstract class DeviceDao {

    @Query("SELECT * FROM devices WHERE stationId = :stationId ORDER BY position")
    abstract fun observeForStation(stationId: String): Flow<List<DeviceEntity>>

    @Query("SELECT updatedAtEpochMillis FROM sync_state WHERE id = :key")
    abstract fun observeUpdatedAt(key: String): Flow<Long?>

    @Query("DELETE FROM devices WHERE stationId = :stationId")
    abstract suspend fun deleteForStation(stationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(devices: List<DeviceEntity>)

    @Upsert
    abstract suspend fun upsertSyncState(state: SyncStateEntity)

    /** Replaces the cached devices of one station with the server response, atomically. */
    @Transaction
    open suspend fun replaceForStation(
        stationId: String,
        devices: List<DeviceEntity>,
        updatedAtEpochMillis: Long,
    ) {
        deleteForStation(stationId)
        insertAll(devices)
        upsertSyncState(SyncStateEntity(SyncKeys.devices(stationId), updatedAtEpochMillis))
    }
}
