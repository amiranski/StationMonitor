package com.amiran.stationmonitor.domain.model

/**
 * Why loading data failed, in terms the UI can explain to the user.
 * The data layer translates exceptions (HttpException, IOException, ...) into these values,
 * so the UI never depends on Retrofit or OkHttp types.
 */
sealed interface DataError {
    /** No route to the server: airplane mode, no Wi-Fi, DNS failure. */
    data object NoConnection : DataError

    /** The server did not answer in time. */
    data object Timeout : DataError

    /** The server answered with an HTTP error code (500, 503, 404, ...). */
    data class Server(val httpCode: Int) : DataError

    /** Anything else, e.g. a response we could not parse. */
    data object Unknown : DataError
}

/** Result of a refresh: the new data itself always arrives through the cache Flow. */
sealed interface RefreshResult {
    data object Success : RefreshResult
    data class Failure(val error: DataError) : RefreshResult
}
