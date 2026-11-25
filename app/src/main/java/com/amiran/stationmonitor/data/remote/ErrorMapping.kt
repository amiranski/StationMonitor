package com.amiran.stationmonitor.data.remote

import com.amiran.stationmonitor.domain.model.DataError
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Translates a technical exception into a [DataError] the UI can show. */
fun Throwable.toDataError(): DataError = when (this) {
    is HttpException -> DataError.Server(code())
    is InterruptedIOException -> DataError.Timeout
    is UnknownHostException, is ConnectException -> DataError.NoConnection
    is java.io.IOException -> DataError.NoConnection
    else -> DataError.Unknown
}
