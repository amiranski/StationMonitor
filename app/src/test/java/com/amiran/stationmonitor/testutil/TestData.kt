package com.amiran.stationmonitor.testutil

import com.amiran.stationmonitor.data.remote.dto.DeviceDto
import com.amiran.stationmonitor.data.remote.dto.StationDto
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.DeviceType
import com.amiran.stationmonitor.domain.model.Station
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Small builders so tests only spell out the fields they care about. */

val FIXED_NOW: Instant = Instant.parse("2026-11-25T21:00:00Z")
val fixedClock: Clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)

fun stationDto(id: String, name: String = "Station $id", deviceCount: Int = 1) =
    StationDto(id = id, name = name, address = "Address $id", country = "CZ", deviceCount = deviceCount)

fun deviceDto(
    id: String,
    type: String = "POS",
    status: String = "ONLINE",
    lastSeen: String? = "2026-11-25T20:59:00Z",
) = DeviceDto(id = id, type = type, name = "Device $id", status = status, lastSeen = lastSeen)

fun station(id: String, name: String = "Station $id") =
    Station(id = id, name = name, address = "Address $id", country = "CZ", deviceCount = 1)

fun device(id: String, status: DeviceStatus, name: String = "Device $id") =
    Device(id = id, stationId = "s1", name = name, type = DeviceType.Pos, status = status, lastSeen = null)

fun httpError(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "{\"error\":{}}".toResponseBody(null)))
