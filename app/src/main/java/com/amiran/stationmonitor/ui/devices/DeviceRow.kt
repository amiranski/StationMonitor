package com.amiran.stationmonitor.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.Device
import com.amiran.stationmonitor.ui.common.formatRelativeTime
import com.amiran.stationmonitor.ui.components.DeviceTypeBadge
import com.amiran.stationmonitor.ui.components.StatusPill
import com.amiran.stationmonitor.ui.components.deviceTypeLabel
import java.time.Instant

/** Where a row sits inside the white card that groups all devices. */
enum class RowPosition { Single, First, Middle, Last }

/**
 * One device: type badge, name, type, "last seen" and the status pill.
 * The rows are separate LazyColumn items but look like one card: only the first and last row
 * get rounded corners, rows in between are separated by a divider.
 */
@Composable
fun DeviceRow(
    device: Device,
    position: RowPosition,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val shape: Shape = when (position) {
        RowPosition.Single -> RoundedCornerShape(20.dp)
        RowPosition.First -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        RowPosition.Middle -> RoundedCornerShape(0.dp)
        RowPosition.Last -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            // TalkBack reads the whole row as one item.
            .semantics(mergeDescendants = true) {},
    ) {
        if (position == RowPosition.Middle || position == RowPosition.Last) {
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeviceTypeBadge(device.type)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = deviceTypeLabel(device.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val lastSeen = device.lastSeen
                Text(
                    text = if (lastSeen == null) {
                        stringResource(R.string.never_seen)
                    } else {
                        stringResource(R.string.last_seen, formatRelativeTime(lastSeen, now))
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                        fontStyle = if (lastSeen == null) FontStyle.Italic else FontStyle.Normal,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(device.status)
        }
    }
}
