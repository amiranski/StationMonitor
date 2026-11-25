package com.amiran.stationmonitor.data.remote

import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.math.pow

/**
 * How often and how fast a failed request is repeated.
 *
 * The mock API fails ~23 % of requests on purpose: 500/503 (usually gone on the next try) and
 * a 30 s silence followed by 504. Each kind of failure gets its own budget:
 * - 5xx and other transient I/O errors: retried [maxServerRetries] times, they are cheap.
 * - Timeouts: retried only [maxTimeoutRetries] time, because every timeout already cost the
 *   user the full timeout (see NetworkModule) of waiting.
 * - No connection, 4xx, unparseable JSON: never retried, repeating cannot help.
 *
 * Delays grow exponentially: initialDelay, initialDelay * factor, ...
 */
data class RetryPolicy(
    val maxServerRetries: Int = 2,
    val maxTimeoutRetries: Int = 1,
    val initialDelayMillis: Long = 500,
    val backoffFactor: Double = 2.0,
) {
    fun delayBeforeRetry(retryNumber: Int): Long =
        (initialDelayMillis * backoffFactor.pow(retryNumber - 1)).toLong()
}

/** What kind of failure an exception represents, from the retry point of view. */
internal enum class FailureKind { SERVER, TIMEOUT, NOT_RETRYABLE }

internal fun Throwable.failureKind(): FailureKind = when (this) {
    is HttpException -> if (code() in 500..599) FailureKind.SERVER else FailureKind.NOT_RETRYABLE
    // OkHttp reports read/call timeouts as SocketTimeoutException / InterruptedIOException.
    is InterruptedIOException -> FailureKind.TIMEOUT
    // The device is offline: retrying right away only makes the user wait longer.
    is UnknownHostException, is ConnectException -> FailureKind.NOT_RETRYABLE
    // Connection reset, unexpected end of stream, ...: usually transient.
    is IOException -> FailureKind.SERVER
    else -> FailureKind.NOT_RETRYABLE
}

/**
 * Runs [block] and repeats it according to [policy].
 *
 * Only the failure kinds above are retried. Anything else, including CancellationException
 * when the screen is closed, is rethrown immediately, so cancellation keeps working.
 */
suspend fun <T> withRetry(policy: RetryPolicy, block: suspend () -> T): T {
    var serverRetries = 0
    var timeoutRetries = 0
    var retryNumber = 0
    while (true) {
        try {
            return block()
        } catch (e: Exception) {
            val canRetry = when (e.failureKind()) {
                FailureKind.SERVER -> serverRetries++ < policy.maxServerRetries
                FailureKind.TIMEOUT -> timeoutRetries++ < policy.maxTimeoutRetries
                FailureKind.NOT_RETRYABLE -> false
            }
            if (!canRetry) throw e
            retryNumber++
            delay(policy.delayBeforeRetry(retryNumber))
        }
    }
}
