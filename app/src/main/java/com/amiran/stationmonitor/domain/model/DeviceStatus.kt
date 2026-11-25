package com.amiran.stationmonitor.domain.model

/**
 * Device health as reported by the backend.
 *
 * The API documents ONLINE | OFFLINE | ERROR and warns that more values may be added. Any value
 * this app version does not know becomes [UNKNOWN] instead of crashing the app.
 */
enum class DeviceStatus {
    ONLINE,
    OFFLINE,
    ERROR,
    UNKNOWN;

    companion object {
        fun fromApi(raw: String?): DeviceStatus {
            val normalized = raw?.trim()?.uppercase() ?: return UNKNOWN
            return entries.firstOrNull { it != UNKNOWN && it.name == normalized } ?: UNKNOWN
        }
    }
}

/**
 * Ordering used on the station detail: problems first, so an operator sees what is broken
 * without scrolling. Inside the same status, devices are sorted by name.
 */
private val DeviceStatus.severity: Int
    get() = when (this) {
        DeviceStatus.ERROR -> 0
        DeviceStatus.UNKNOWN -> 1
        DeviceStatus.OFFLINE -> 2
        DeviceStatus.ONLINE -> 3
    }

fun List<Device>.sortedBySeverity(): List<Device> =
    sortedWith(compareBy<Device>({ it.status.severity }, { it.name.lowercase() }))
