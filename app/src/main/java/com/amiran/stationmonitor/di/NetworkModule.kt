package com.amiran.stationmonitor.di

import com.amiran.stationmonitor.BuildConfig
import com.amiran.stationmonitor.data.remote.RetryPolicy
import com.amiran.stationmonitor.data.remote.StationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Tells Hilt how to build the networking objects. They are classes from libraries, so they
 * cannot get an @Inject constructor; @Provides functions are the "recipes" instead.
 * SingletonComponent + @Singleton = one instance for the whole app.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // New fields added on the backend must not break parsing.
        ignoreUnknownKeys = true
        // A missing nullable field (e.g. "lastSeen") becomes null instead of an error.
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        // OkHttp's defaults are 10 s per phase with no limit for the whole call. The mock API
        // sometimes stays silent for 30 s before answering 504, so waiting for it is pointless.
        // Slow but valid answers (2-5 s) still fit comfortably.
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                // Logs "--> GET url" / "<-- 503 (120 ms)" lines to Logcat, debug builds only.
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideStationApi(retrofit: Retrofit): StationApi = retrofit.create(StationApi::class.java)

    @Provides
    fun provideRetryPolicy(): RetryPolicy = RetryPolicy()
}
