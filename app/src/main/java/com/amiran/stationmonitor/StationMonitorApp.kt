package com.amiran.stationmonitor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The Application object is created before any screen. @HiltAndroidApp makes Hilt generate
 * the app-wide dependency container here. Registered in AndroidManifest.xml (android:name).
 */
@HiltAndroidApp
class StationMonitorApp : Application()
