package com.amiran.stationmonitor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amiran.stationmonitor.data.local.dao.DeviceDao
import com.amiran.stationmonitor.data.local.dao.StationDao
import com.amiran.stationmonitor.data.local.entity.DeviceEntity
import com.amiran.stationmonitor.data.local.entity.StationEntity
import com.amiran.stationmonitor.data.local.entity.SyncStateEntity

/**
 * The SQLite database used as the offline cache and as the single source of truth for the UI.
 * The schema of every version is exported to app/schemas (see the `room {}` block in
 * app/build.gradle.kts) so later migrations can be written against it.
 */
@Database(
    entities = [StationEntity::class, DeviceEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class StationMonitorDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    abstract fun deviceDao(): DeviceDao

    companion object {
        const val NAME = "station-monitor.db"
    }
}
