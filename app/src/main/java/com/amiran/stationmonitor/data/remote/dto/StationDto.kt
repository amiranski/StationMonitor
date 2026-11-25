package com.amiran.stationmonitor.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One item of `GET /api/v1/stations`, field for field as the JSON looks.
 * DTO = Data Transfer Object: it describes the wire format and nothing else.
 */
@Serializable
data class StationDto(
    val id: String,
    val name: String,
    val address: String,
    val country: String,
    val deviceCount: Int,
)
