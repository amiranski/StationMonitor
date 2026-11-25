package com.amiran.stationmonitor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row of the `devices` table.
 *
 * `type` and `status` are stored raw, exactly as the API sent them. They are converted to
 * domain types when reading, so if a future app version learns a new status, the cached data
 * shows it correctly without downloading again.
 */
@Entity(
    tableName = "devices",
    indices = [Index("stationId")],
)
data class DeviceEntity(
    @PrimaryKey val id: String,
    val stationId: String,
    val name: String,
    val type: String,
    val status: String,
    /** Epoch milliseconds (UTC) or null when the device was never seen. */
    val lastSeenEpochMillis: Long?,
    /** Index in the server response. */
    val position: Int,
)
