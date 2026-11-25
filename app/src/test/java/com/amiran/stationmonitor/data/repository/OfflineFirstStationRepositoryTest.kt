package com.amiran.stationmonitor.data.repository

import com.amiran.stationmonitor.data.local.entity.SyncKeys
import com.amiran.stationmonitor.data.remote.RetryPolicy
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.RefreshResult
import com.amiran.stationmonitor.testutil.FIXED_NOW
import com.amiran.stationmonitor.testutil.FakeDeviceDao
import com.amiran.stationmonitor.testutil.FakeStationApi
import com.amiran.stationmonitor.testutil.FakeStationDao
import com.amiran.stationmonitor.testutil.FakeTables
import com.amiran.stationmonitor.testutil.deviceDto
import com.amiran.stationmonitor.testutil.fixedClock
import com.amiran.stationmonitor.testutil.httpError
import com.amiran.stationmonitor.testutil.stationDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class OfflineFirstStationRepositoryTest {

    private val tables = FakeTables()
    private val api = FakeStationApi()
    private val repository = OfflineFirstStationRepository(
        api = api,
        stationDao = FakeStationDao(tables),
        deviceDao = FakeDeviceDao(tables),
        retryPolicy = RetryPolicy(),
        clock = fixedClock,
    )

    @Test
    fun `nothing cached yet - observe emits an empty list with no update time`() = runTest {
        val cached = repository.observeStations().first()

        assertTrue(cached.value.isEmpty())
        assertNull(cached.updatedAt)
    }

    @Test
    fun `successful refresh stores stations in server order and the update time`() = runTest {
        api.stationResponses += Result.success(listOf(stationDto("b"), stationDto("a")))

        val result = repository.refreshStations()

        assertEquals(RefreshResult.Success, result)
        val cached = repository.observeStations().first()
        assertEquals(listOf("b", "a"), cached.value.map { it.id })
        assertEquals(FIXED_NOW, cached.updatedAt)
    }

    @Test
    fun `transient server errors are retried and still succeed`() = runTest {
        api.stationResponses += Result.failure(httpError(503))
        api.stationResponses += Result.failure(httpError(500))
        api.stationResponses += Result.success(listOf(stationDto("a")))

        val result = repository.refreshStations()

        assertEquals(RefreshResult.Success, result)
        assertEquals(3, api.stationCalls)
    }

    @Test
    fun `failed refresh keeps the cached data untouched and reports the error`() = runTest {
        api.stationResponses += Result.success(listOf(stationDto("a")))
        repository.refreshStations()
        api.stationResponses += Result.failure(UnknownHostException())

        val result = repository.refreshStations()

        assertEquals(RefreshResult.Failure(DataError.NoConnection), result)
        val cached = repository.observeStations().first()
        assertEquals(listOf("a"), cached.value.map { it.id })
        assertEquals(FIXED_NOW, cached.updatedAt)
    }

    @Test
    fun `stations removed on the server disappear from the cache together with their devices`() = runTest {
        api.stationResponses += Result.success(listOf(stationDto("a"), stationDto("b")))
        repository.refreshStations()
        api.deviceResponses += Result.success(listOf(deviceDto("d1")))
        repository.refreshDevices("b")

        api.stationResponses += Result.success(listOf(stationDto("a")))
        repository.refreshStations()

        assertEquals(listOf("a"), repository.observeStations().first().value.map { it.id })
        assertTrue(tables.devices.value.isEmpty())
    }

    @Test
    fun `device refresh replaces the station's devices and keeps other stations' devices`() = runTest {
        api.deviceResponses += Result.success(listOf(deviceDto("old1"), deviceDto("old2")))
        repository.refreshDevices("s1")
        api.deviceResponses += Result.success(listOf(deviceDto("x1")))
        repository.refreshDevices("s2")

        api.deviceResponses += Result.success(listOf(deviceDto("new1", status = "ERROR")))
        repository.refreshDevices("s1")

        val s1 = repository.observeDevices("s1").first()
        assertEquals(listOf("new1"), s1.value.map { it.id })
        assertEquals(FIXED_NOW, s1.updatedAt)
        assertEquals(listOf("x1"), repository.observeDevices("s2").first().value.map { it.id })
        assertEquals(FIXED_NOW.toEpochMilli(), tables.syncState.value[SyncKeys.devices("s1")])
    }

    @Test
    fun `server returning an empty device list is cached as loaded-but-empty`() = runTest {
        api.deviceResponses += Result.success(emptyList())

        repository.refreshDevices("s1")

        val cached = repository.observeDevices("s1").first()
        assertTrue(cached.value.isEmpty())
        assertEquals(FIXED_NOW, cached.updatedAt)
    }
}
