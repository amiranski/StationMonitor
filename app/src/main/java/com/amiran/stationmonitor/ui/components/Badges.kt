package com.amiran.stationmonitor.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.DeviceType

/**
 * Outlined device-type code ("POS"), styled like the fuel badges ("E10", "B7") in the
 * company's app. An unknown type keeps its raw code but gets a dashed border.
 */
@Composable
fun DeviceTypeBadge(type: DeviceType, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    val border = if (type is DeviceType.Unknown) {
        Modifier.dashedBorder(color, cornerRadius = 8.dp)
    } else {
        Modifier.border(1.5.dp, color, RoundedCornerShape(8.dp))
    }
    Box(
        modifier = modifier
            .height(30.dp)
            .defaultMinSize(minWidth = 46.dp)
            .widthIn(max = 72.dp)
            .then(border)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.code,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small outlined country code, e.g. "CZ". */
@Composable
fun CountryBadge(country: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .border(1.5.dp, color, RoundedCornerShape(5.dp))
            .padding(horizontal = 4.dp),
    ) {
        Text(text = country, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
fun deviceTypeLabel(type: DeviceType): String = stringResource(
    when (type) {
        DeviceType.Pos -> R.string.device_type_pos
        DeviceType.Bos -> R.string.device_type_bos
        DeviceType.Fcs -> R.string.device_type_fcs
        DeviceType.Opt -> R.string.device_type_opt
        is DeviceType.Unknown -> R.string.device_type_unknown
    },
)

private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        ),
    )
}
