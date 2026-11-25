package com.amiran.stationmonitor.data.mapper

import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.DeviceType
import com.amiran.stationmonitor.testutil.deviceDto
import com.amiran.stationmonitor.testutil.stationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MappersTest {

    @Test
    fun `station dto survives the trip to the domain model`() {
        val dto = stationDto("abc", name = "Test 🚗⛽ & <special> \"chars\"", deviceCount = 42)

        val station = dto.toEntity(position = 7).toDomain()

        assertEquals("abc", station.id)
        assertEquals("Test 🚗⛽ & <special> \"chars\"", station.name)
        assertEquals(42, station.deviceCount)
    }

    @Test
    fun `entity keeps server order`() {
        assertEquals(7, stationDto("abc").toEntity(position = 7).position)
    }

    @Test
    fun `device lastSeen is parsed from ISO UTC`() {
        val device = deviceDto("d1", lastSeen = "2026-11-25T21:03:00Z").toEntity("s1", 0).toDomain()

        assertEquals(Instant.parse("2026-11-25T21:03:00Z"), device.lastSeen)
        assertEquals("s1", device.stationId)
    }

    @Test
    fun `null or malformed lastSeen becomes null instead of failing`() {
        assertNull(deviceDto("d1", lastSeen = null).toEntity("s1", 0).toDomain().lastSeen)
        assertNull(deviceDto("d1", lastSeen = "yesterday").toEntity("s1", 0).toDomain().lastSeen)
    }

    @Test
    fun `unknown status and type are kept raw in the cache and mapped on read`() {
        val entity = deviceDto("d1", type = "EVC", status = "MAINTENANCE").toEntity("s1", 0)

        assertEquals("EVC", entity.type)
        assertEquals("MAINTENANCE", entity.status)
        val device = entity.toDomain()
        assertEquals(DeviceType.Unknown("EVC"), device.type)
        assertEquals(DeviceStatus.UNKNOWN, device.status)
    }
}
