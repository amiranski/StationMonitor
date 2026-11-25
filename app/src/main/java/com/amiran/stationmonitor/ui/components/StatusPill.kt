package com.amiran.stationmonitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.DeviceStatus
import com.amiran.stationmonitor.ui.theme.StationMonitorTheme
import com.amiran.stationmonitor.ui.theme.StatusColor
import com.amiran.stationmonitor.ui.theme.statusColors

/** Rounded pill with a colored dot and the status name, e.g. "● Online" or "● 3 Online". */
@Composable
fun StatusPill(
    status: DeviceStatus,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val colors = status.colors()
    val label = statusLabel(status)
    val text = if (count == null) label else stringResource(R.string.status_count, count, label)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colors.dot),
        )
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = colors.content)
    }
}

@Composable
fun statusLabel(status: DeviceStatus): String = stringResource(
    when (status) {
        DeviceStatus.ONLINE -> R.string.status_online
        DeviceStatus.OFFLINE -> R.string.status_offline
        DeviceStatus.ERROR -> R.string.status_error
        DeviceStatus.UNKNOWN -> R.string.status_unknown
    },
)

@Composable
private fun DeviceStatus.colors(): StatusColor {
    val palette = MaterialTheme.statusColors
    return when (this) {
        DeviceStatus.ONLINE -> palette.online
        DeviceStatus.OFFLINE -> palette.offline
        DeviceStatus.ERROR -> palette.error
        DeviceStatus.UNKNOWN -> palette.unknown
    }
}

@Preview
@Composable
private fun StatusPillPreview() {
    StationMonitorTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeviceStatus.entries.forEach { StatusPill(it) }
        }
    }
}
