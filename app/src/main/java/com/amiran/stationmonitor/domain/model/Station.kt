package com.amiran.stationmonitor.domain.model

/**
 * A fuel station as the rest of the app sees it.
 *
 * Domain models are plain Kotlin: no JSON annotations (that is the DTO's job) and no Room
 * annotations (that is the Entity's job). The UI and ViewModels only ever see these classes.
 */
data class Station(
    val id: String,
    val name: String,
    val address: String,
    /** ISO country code as sent by the API, e.g. "CZ", "SK". */
    val country: String,
    /** Number of devices according to the station list endpoint. */
    val deviceCount: Int,
)
