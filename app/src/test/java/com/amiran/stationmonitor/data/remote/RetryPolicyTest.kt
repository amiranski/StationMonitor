package com.amiran.stationmonitor.data.remote

import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.testutil.httpError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * runTest uses virtual time: `delay` inside withRetry does not really wait, and
 * `currentTime` tells how long the backoff would have taken.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryPolicyTest {

    private val policy = RetryPolicy(maxServerRetries = 2, maxTimeoutRetries = 1, initialDelayMillis = 500)

    @Test
    fun `503, 503, then success returns the result after two retries`() = runTest {
        var calls = 0
        val result = withRetry(policy) {
            calls++
            if (calls <= 2) throw httpError(503)
            "data"
        }

        assertEquals("data", result)
        assertEquals(3, calls)
        // Exponential backoff: 500 ms + 1000 ms.
        assertEquals(1_500, currentTime)
    }

    @Test
    fun `server errors give up after the retry budget`() = runTest {
        var calls = 0
        val error = httpError(500)
        try {
            withRetry(policy) {
                calls++
                throw error
            }
            fail("Expected HttpException")
        } catch (e: Exception) {
            assertSame(error, e)
        }
        assertEquals(1 + policy.maxServerRetries, calls)
    }

    @Test
    fun `timeouts are retried only once`() = runTest {
        var calls = 0
        try {
            withRetry(policy) {
                calls++
                throw SocketTimeoutException("timeout")
            }
            fail("Expected SocketTimeoutException")
        } catch (e: SocketTimeoutException) {
            // expected
        }
        assertEquals(2, calls)
    }

    @Test
    fun `client errors, no connection and bad JSON are not retried`() = runTest {
        listOf(httpError(404), UnknownHostException(), SerializationException("bad json")).forEach { error ->
            var calls = 0
            try {
                withRetry(policy) {
                    calls++
                    throw error
                }
            } catch (e: Exception) {
                assertSame(error, e)
            }
            assertEquals("$error must not be retried", 1, calls)
        }
    }

    @Test
    fun `exceptions are mapped to user-facing errors`() {
        assertEquals(DataError.Server(503), httpError(503).toDataError())
        assertEquals(DataError.Timeout, SocketTimeoutException().toDataError())
        assertEquals(DataError.NoConnection, UnknownHostException().toDataError())
        assertEquals(DataError.Unknown, SerializationException("x").toDataError())
    }
}
