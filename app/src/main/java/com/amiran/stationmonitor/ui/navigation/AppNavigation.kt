package com.amiran.stationmonitor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.amiran.stationmonitor.ui.devices.DevicesRoute
import com.amiran.stationmonitor.ui.stations.StationsRoute
import kotlinx.serialization.Serializable

/*
 * Navigation 3: the back stack is a plain list of keys that we own, like
 * `NavigationStack(path: $path)` in SwiftUI. Opening a screen = add a key, going back =
 * remove the last one. Keys are @Serializable so the stack survives process death.
 */

/** The stations list (start destination). */
@Serializable
data object StationsKey : NavKey

/** Detail of one station. The argument is typed: no string routes, no manual parsing. */
@Serializable
data class StationDetailKey(val stationId: String) : NavKey

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(StationsKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            // Keeps rememberSaveable state (e.g. scroll position) per entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Gives every entry its own ViewModelStore: the ViewModel lives exactly as long as
            // its screen is on the back stack.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<StationsKey> {
                StationsRoute(onStationClick = { stationId -> backStack.add(StationDetailKey(stationId)) })
            }
            entry<StationDetailKey> { key ->
                DevicesRoute(stationId = key.stationId, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
