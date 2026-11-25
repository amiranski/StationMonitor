package com.amiran.stationmonitor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A row of the `stations` table: the cached station list. */
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val country: String,
    val deviceCount: Int,
    /** Index in the server response, so the cached list keeps the server's order. */
    val position: Int,
)
