package com.amiran.stationmonitor.domain.model

import java.time.Instant

/** A device installed at a station (cash register, back-office server, ...). */
data class Device(
    val id: String,
    val stationId: String,
    val name: String,
    val type: DeviceType,
    val status: DeviceStatus,
    /** When the device last reported in. `null` = the backend has never seen it. */
    val lastSeen: Instant?,
)
