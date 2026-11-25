package com.amiran.stationmonitor.data.remote

import com.amiran.stationmonitor.data.remote.dto.DeviceDto
import com.amiran.stationmonitor.data.remote.dto.StationDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * The backend API described as a Kotlin interface. Retrofit generates the implementation at
 * runtime. `suspend` functions run the request without blocking the calling thread and throw
 * `HttpException` for non-2xx responses or `IOException` for network problems.
 */
interface StationApi {

    @GET("api/v1/stations")
    suspend fun getStations(): List<StationDto>

    @GET("api/v1/stations/{id}/devices")
    suspend fun getDevices(@Path("id") stationId: String): List<DeviceDto>
}
