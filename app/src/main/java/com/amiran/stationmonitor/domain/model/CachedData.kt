package com.amiran.stationmonitor.domain.model

import java.time.Instant

/**
 * A value read from the local cache together with the time it was last refreshed from the
 * network.
 *
 * [updatedAt] `== null` means "never downloaded yet". This is how the UI tells apart
 * "still loading for the first time" from "the server really returned an empty list".
 */
data class CachedData<out T>(
    val value: T,
    val updatedAt: Instant?,
)
