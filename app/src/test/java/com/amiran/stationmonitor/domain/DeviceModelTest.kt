package com.amiran.stationmonitor.domain

import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.DeviceType
import com.amiran.stationmonitor.domain.model.sortedBySeverity
import com.amiran.stationmonitor.testutil.device
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceModelTest {

    @Test
    fun `known statuses are parsed, case and whitespace insensitive`() {
        assertEquals(DeviceStatus.ONLINE, DeviceStatus.fromApi("ONLINE"))
        assertEquals(DeviceStatus.OFFLINE, DeviceStatus.fromApi(" offline "))
        assertEquals(DeviceStatus.ERROR, DeviceStatus.fromApi("Error"))
    }

    @Test
    fun `new or missing statuses become UNKNOWN instead of crashing`() {
        assertEquals(DeviceStatus.UNKNOWN, DeviceStatus.fromApi("MAINTENANCE"))
        assertEquals(DeviceStatus.UNKNOWN, DeviceStatus.fromApi(""))
        assertEquals(DeviceStatus.UNKNOWN, DeviceStatus.fromApi(null))
        // The literal value "UNKNOWN" from the server is unknown too, not a special case.
        assertEquals(DeviceStatus.UNKNOWN, DeviceStatus.fromApi("UNKNOWN"))
    }

    @Test
    fun `known device types are parsed`() {
        assertEquals(DeviceType.Pos, DeviceType.fromApi("POS"))
        assertEquals(DeviceType.Bos, DeviceType.fromApi("bos"))
        assertEquals(DeviceType.Fcs, DeviceType.fromApi("FCS"))
        assertEquals(DeviceType.Opt, DeviceType.fromApi("OPT"))
    }

    @Test
    fun `new device type keeps its raw code`() {
        assertEquals(DeviceType.Unknown("EVC"), DeviceType.fromApi("EVC"))
        assertEquals(DeviceType.Unknown("?"), DeviceType.fromApi(null))
    }

    @Test
    fun `devices are sorted problems first, then by name`() {
        val devices = listOf(
            device("1", DeviceStatus.ONLINE, name = "b"),
            device("2", DeviceStatus.OFFLINE),
            device("3", DeviceStatus.ERROR),
            device("4", DeviceStatus.ONLINE, name = "a"),
            device("5", DeviceStatus.UNKNOWN),
        )

        val ids = devices.sortedBySeverity().map { it.id }

        assertEquals(listOf("3", "5", "2", "4", "1"), ids)
    }
}
