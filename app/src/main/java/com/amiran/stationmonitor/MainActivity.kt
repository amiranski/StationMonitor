package com.amiran.stationmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amiran.stationmonitor.ui.navigation.AppNavigation
import com.amiran.stationmonitor.ui.theme.StationMonitorTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The only Activity (single-activity architecture): every screen is a composable switched by
 * Navigation 3. @AndroidEntryPoint lets Hilt inject ViewModels into the screens it hosts.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; Scaffold adds the needed insets.
        enableEdgeToEdge()
        setContent {
            StationMonitorTheme {
                AppNavigation()
            }
        }
    }
}
