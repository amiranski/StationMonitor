package com.amiran.stationmonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Remembers when a piece of data was last downloaded successfully, e.g. for the
 * "Showing saved data · from 17:28" banner. One row per cached list, see [SyncKeys].
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: String,
    val updatedAtEpochMillis: Long,
)

object SyncKeys {
    const val STATIONS = "stations"

    fun devices(stationId: String) = "devices:$stationId"
}
