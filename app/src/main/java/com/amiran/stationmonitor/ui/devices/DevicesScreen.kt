package com.amiran.stationmonitor.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.domain.model.DeviceType
import com.amiran.stationmonitor.domain.model.Station
import com.amiran.stationmonitor.ui.common.ListUiState
import com.amiran.stationmonitor.ui.common.formatUpdatedAt
import com.amiran.stationmonitor.ui.common.message
import com.amiran.stationmonitor.ui.common.rememberNow
import com.amiran.stationmonitor.ui.components.CountryBadge
import com.amiran.stationmonitor.ui.components.ErrorSnackbarEffect
import com.amiran.stationmonitor.ui.components.FullScreenMessage
import com.amiran.stationmonitor.ui.components.MessageActionStyle
import com.amiran.stationmonitor.ui.components.SkeletonRow
import com.amiran.stationmonitor.ui.components.SkeletonWidths
import com.amiran.stationmonitor.ui.components.StaleDataBanner
import com.amiran.stationmonitor.ui.components.StatusPill
import com.amiran.stationmonitor.ui.theme.StationMonitorTheme
import java.time.Duration
import java.time.Instant

@Composable
fun DevicesRoute(
    stationId: String,
    onBack: () -> Unit,
) {
    // hiltViewModel with an assisted factory: Hilt creates the ViewModel and passes the id.
    // Each back stack entry has its own ViewModelStore (see AppNavigation), so two different
    // stations never share a ViewModel.
    val viewModel = hiltViewModel<DevicesViewModel, DevicesViewModel.Factory>(
        creationCallback = { factory -> factory.create(stationId) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DevicesScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onErrorShown = viewModel::onErrorShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    state: DevicesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = rememberNow(),
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.station?.name ?: stringResource(R.string.station_fallback_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val list = state.list
        val isRefreshing = when (list) {
            is ListUiState.Content -> list.isRefreshing
            is ListUiState.Empty -> list.isRefreshing
            else -> false
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                state.station?.let { station ->
                    item(key = "station") {
                        StationHeaderCard(
                            station = station,
                            statusCounts = state.statusCounts,
                            updatedAt = (list as? ListUiState.Content)?.updatedAt,
                        )
                    }
                }

                when (list) {
                    ListUiState.Loading -> {
                        item(key = "progress") {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp))
                        }
                        items(SkeletonWidths.take(4)) { width -> SkeletonRow(width, Modifier.padding(bottom = 10.dp)) }
                    }

                    is ListUiState.Content -> {
                        if (list.isStale) {
                            item(key = "stale") { StaleDataBanner(list.updatedAt, Modifier.padding(top = 12.dp)) }
                        }
                        item(key = "section") { DevicesSectionLabel(count = list.items.size) }
                        itemsIndexed(list.items, key = { _, device -> device.id }) { index, device ->
                            DeviceRow(
                                device = device,
                                position = rowPosition(index, list.items.size),
                                now = now,
                            )
                        }
                    }

                    is ListUiState.Empty -> item(key = "empty") {
                        FullScreenMessage(
                            icon = R.drawable.ic_devices,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
                            title = stringResource(R.string.devices_empty_title),
                            body = stringResource(R.string.devices_empty_body),
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
                            title = stringResource(R.string.devices_error_title),
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

/**
 * Full station name and address (both wrap, nothing is cut off here), the status summary and
 * the time of the last successful download.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StationHeaderCard(
    station: Station,
    statusCounts: List<Pair<DeviceStatus, Int>>,
    updatedAt: Instant?,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountryBadge(station.country, Modifier.padding(top = 2.dp))
                Text(
                    text = station.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (statusCounts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    statusCounts.forEach { (status, count) -> StatusPill(status, count = count) }
                }
            }
            if (updatedAt != null) {
                Text(
                    text = stringResource(R.string.updated_at, formatUpdatedAt(updatedAt)),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = MaterialTheme.typography.bodySmall.fontWeight),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DevicesSectionLabel(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.devices_section).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun rowPosition(index: Int, size: Int): RowPosition = when {
    size == 1 -> RowPosition.Single
    index == 0 -> RowPosition.First
    index == size - 1 -> RowPosition.Last
    else -> RowPosition.Middle
}

@Preview(name = "Devices")
@Composable
private fun DevicesContentPreview() {
    val now = Instant.now()
    val devices = listOf(
        Device("1", "s", "Pokladna 2", DeviceType.Pos, DeviceStatus.ERROR, now.minus(Duration.ofMinutes(55))),
        Device("2", "s", "Venkovní terminál", DeviceType.Opt, DeviceStatus.UNKNOWN, now.minus(Duration.ofHours(3))),
        Device("3", "s", "Backoffice", DeviceType.Bos, DeviceStatus.OFFLINE, null),
        Device("4", "s", "Forecourt server", DeviceType.Fcs, DeviceStatus.ONLINE, now.minus(Duration.ofMinutes(2))),
        Device("5", "s", "EV charger 1", DeviceType.Unknown("EVC"), DeviceStatus.ONLINE, now),
    )
    StationMonitorTheme {
        DevicesScreen(
            state = DevicesUiState(
                station = Station("s", "Praha – Smíchov", "Strakonická 1199/2a, 150 00 Praha 5", "CZ", 5),
                list = ListUiState.Content(devices, now, isRefreshing = false, isStale = false),
                statusCounts = statusCounts(devices),
            ),
            onBack = {},
            onRefresh = {},
            onErrorShown = {},
            now = now,
        )
    }
}
