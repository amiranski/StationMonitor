package com.amiran.stationmonitor.data.mapper

import com.amiran.stationmonitor.data.local.entity.DeviceEntity
import com.amiran.stationmonitor.data.local.entity.StationEntity
import com.amiran.stationmonitor.data.remote.dto.DeviceDto
import com.amiran.stationmonitor.data.remote.dto.StationDto
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.DeviceType
import com.amiran.stationmonitor.domain.model.Station
import java.time.Instant
import java.time.format.DateTimeParseException

/*
 * Conversions between the three model layers:
 *   network (DTO)  ->  cache (Entity)  ->  app (domain model)
 * Written as extension functions: `dto.toEntity(...)` reads like a method of the DTO without
 * the DTO class knowing anything about Room.
 */

fun StationDto.toEntity(position: Int) = StationEntity(
    id = id,
    name = name,
    address = address,
    country = country,
    deviceCount = deviceCount,
    position = position,
)

fun DeviceDto.toEntity(stationId: String, position: Int) = DeviceEntity(
    id = id,
    stationId = stationId,
    name = name,
    type = type,
    status = status,
    lastSeenEpochMillis = parseIsoInstantOrNull(lastSeen)?.toEpochMilli(),
    position = position,
)

fun StationEntity.toDomain() = Station(
    id = id,
    name = name,
    address = address,
    country = country,
    deviceCount = deviceCount,
)

fun DeviceEntity.toDomain() = Device(
    id = id,
    stationId = stationId,
    name = name,
    type = DeviceType.fromApi(type),
    status = DeviceStatus.fromApi(status),
    lastSeen = lastSeenEpochMillis?.let(Instant::ofEpochMilli),
)

/** A malformed timestamp is treated like a missing one instead of failing the whole list. */
internal fun parseIsoInstantOrNull(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        Instant.parse(value)
    } catch (e: DateTimeParseException) {
        null
    }
}
