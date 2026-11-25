package com.amiran.stationmonitor.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One item of `GET /api/v1/stations/{id}/devices`.
 *
 * `type` and `status` are deliberately plain Strings, not enums: the API says new values may
 * appear, and an enum would make the whole response fail to parse. They are turned into
 * domain types (with an UNKNOWN fallback) in the mapper.
 */
@Serializable
data class DeviceDto(
    val id: String,
    val type: String,
    val name: String,
    val status: String,
    /** ISO-8601 UTC timestamp, e.g. "2026-11-25T21:03:00Z", or null. */
    val lastSeen: String? = null,
)
