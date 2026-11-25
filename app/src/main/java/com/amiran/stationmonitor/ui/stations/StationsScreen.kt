package com.amiran.stationmonitor.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.ui.common.ListUiState
import com.amiran.stationmonitor.ui.common.formatUpdatedAt
import com.amiran.stationmonitor.ui.common.message
import com.amiran.stationmonitor.ui.components.ErrorSnackbarEffect
import com.amiran.stationmonitor.ui.components.FullScreenMessage
import com.amiran.stationmonitor.ui.components.MessageActionStyle
import com.amiran.stationmonitor.ui.components.SkeletonRow
import com.amiran.stationmonitor.ui.components.SkeletonWidths
import com.amiran.stationmonitor.ui.components.StaleDataBanner
import com.amiran.stationmonitor.ui.theme.StationMonitorTheme
import java.time.Instant

/**
 * Connects the screen to its ViewModel. Kept separate from [StationsScreen] so the screen
 * itself only takes plain data and lambdas: easy to preview and to test without Hilt.
 */
@Composable
fun StationsRoute(
    onStationClick: (stationId: String) -> Unit,
    viewModel: StationsViewModel = hiltViewModel(),
) {
    // Lifecycle-aware: stops collecting while the app is in the background.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    StationsScreen(
        state = state,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onCountryToggle = viewModel::onCountryToggle,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onStationClick = onStationClick,
        onErrorShown = viewModel::onErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    state: StationsUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onCountryToggle: (code: String) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onStationClick: (stationId: String) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        error = state.snackbarError,
        hostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onErrorShown,
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val list = state.list
        val isRefreshing = when (list) {
            is ListUiState.Content -> list.isRefreshing
            is ListUiState.Empty -> list.isRefreshing
            else -> false
        }
        // The list is always a LazyColumn, even for empty/error states, so pull-to-refresh
        // works everywhere.
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") { StationsHeader(list, isFiltered = state.filter.isActive, totalCount = state.totalCount) }

                when (list) {
                    ListUiState.Loading -> {
                        item(key = "progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        items(SkeletonWidths) { width -> SkeletonRow(width) }
                    }

                    is ListUiState.Content -> {
                        if (list.isStale) {
                            item(key = "stale") { StaleDataBanner(list.updatedAt) }
                        }
                        // Fixed keys keep the search field (and its keyboard focus) in place
                        // while the rows below it change with every typed character.
                        item(key = "search") { StationSearchField(query = query, onQueryChange = onQueryChange) }
                        if (state.countries.size > 1) {
                            item(key = "countries") {
                                CountryFilterChips(countries = state.countries, onToggle = onCountryToggle)
                            }
                        }
                        if (state.isNoResults) {
                            item(key = "no_results") {
                                FullScreenMessage(
                                    icon = R.drawable.ic_search,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    iconBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    title = stringResource(R.string.no_results_title),
                                    body = stringResource(R.string.no_results_body),
                                    actionLabel = stringResource(R.string.action_clear_filters),
                                    onAction = onClearFilters,
                                    actionStyle = MessageActionStyle.Secondary,
                                )
                            }
                        }
                        // key = stable id: Compose can keep each row's state and animate
                        // changes instead of redrawing the whole list.
                        items(list.items, key = { it.id }) { station ->
                            StationRow(station = station, onClick = { onStationClick(station.id) })
                        }
                    }

                    is ListUiState.Empty -> item(key = "empty") {
                        FullScreenMessage(
                            icon = R.drawable.ic_gas_station,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
                            title = stringResource(R.string.stations_empty_title),
                            body = stringResource(R.string.stations_empty_body),
                            actionLabel = stringResource(R.string.action_refresh),
                            onAction = onRefresh,
                            actionStyle = MessageActionStyle.Secondary,
                            isActionLoading = list.isRefreshing,
                        )
                    }

                    is ListUiState.Error -> item(key = "error") {
                        FullScreenMessage(
                            icon = R.drawable.ic_cloud_off,
                            iconTint = MaterialTheme.colorScheme.error,
                            iconBackground = MaterialTheme.colorScheme.errorContainer,
                            title = stringResource(R.string.stations_error_title),
                            body = list.error.message() + " " + stringResource(R.string.error_hint),
                            actionLabel = stringResource(R.string.action_try_again),
                            onAction = onRefresh,
                            isActionLoading = list.isRetrying,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StationsHeader(list: ListUiState<Station>, isFiltered: Boolean, totalCount: Int) {
    Column(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.stations_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        val subtitle = when (list) {
            ListUiState.Loading -> stringResource(R.string.loading_stations)
            is ListUiState.Content -> {
                val shown = list.items.size
                val count = if (isFiltered) {
                    stringResource(R.string.stations_filtered_count, shown, totalCount)
                } else {
                    pluralStringResource(R.plurals.stations_count, shown, shown)
                }
                list.updatedAt?.let { stringResource(R.string.list_summary_updated, count, formatUpdatedAt(it)) } ?: count
            }
            else -> null
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val previewStations = listOf(
    Station("1", "Praha – Chodov", "Roztylská 2321/19, 148 00 Praha 11", "CZ", 3),
    Station("2", "Praha – Smíchov", "Strakonická 1199/2a, 150 00 Praha 5", "CZ", 6),
    Station("3", "D1 Humpolec – odpočívka (směr Brno)", "Dálnice D1, km 90, 396 01 Humpolec", "CZ", 42),
    Station("4", "Bratislava – Petržalka", "Panónska cesta 2, 851 04 Bratislava", "SK", 4),
)

@Preview(name = "Content, stale")
@Composable
private fun StationsContentPreview() {
    StationMonitorTheme {
        StationsScreen(
            state = StationsUiState(
                list = ListUiState.Content(previewStations, Instant.now(), isRefreshing = false, isStale = true),
                countries = previewStations.countryOptions(selected = emptySet()),
                totalCount = previewStations.size,
            ),
            query = "",
            onQueryChange = {},
            onCountryToggle = {},
            onClearFilters = {},
            onRefresh = {},
            onStationClick = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "Filtered, no results")
@Composable
private fun StationsNoResultsPreview() {
    val filter = StationFilter(query = "brno", countries = setOf("SK"))
    StationMonitorTheme {
        StationsScreen(
            state = StationsUiState(
                list = ListUiState.Content(
                    previewStations.matching(filter), Instant.now(), isRefreshing = false, isStale = false,
                ),
                filter = filter,
                countries = previewStations.countryOptions(selected = filter.countries),
                totalCount = previewStations.size,
            ),
            query = filter.query,
            onQueryChange = {},
            onCountryToggle = {},
            onClearFilters = {},
            onRefresh = {},
            onStationClick = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "Loading")
@Composable
private fun StationsLoadingPreview() {
    StationMonitorTheme {
        StationsScreen(
            state = StationsUiState(ListUiState.Loading),
            query = "",
            onQueryChange = {},
            onCountryToggle = {},
            onClearFilters = {},
            onRefresh = {},
            onStationClick = {},
            onErrorShown = {},
        )
    }
}

@Preview(name = "Error without cache")
@Composable
private fun StationsErrorPreview() {
    StationMonitorTheme {
        StationsScreen(
            state = StationsUiState(ListUiState.Error(DataError.Server(503), isRetrying = false)),
            query = "",
            onQueryChange = {},
            onCountryToggle = {},
            onClearFilters = {},
            onRefresh = {},
            onStationClick = {},
            onErrorShown = {},
        )
    }
}
