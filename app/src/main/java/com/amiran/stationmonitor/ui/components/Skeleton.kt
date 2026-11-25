package com.amiran.stationmonitor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Grey placeholder shaped like a station row, shown during the first load. It pulses so the
 * screen does not look frozen. A real progress bar is shown above it as well.
 */
@Composable
fun SkeletonRow(widthFraction: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).background(placeholder, RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(widthFraction).height(14.dp).background(placeholder, RoundedCornerShape(7.dp)))
            Box(Modifier.fillMaxWidth(0.85f).height(10.dp).background(placeholder, RoundedCornerShape(5.dp)))
        }
        Box(Modifier.width(56.dp).height(22.dp).background(placeholder, RoundedCornerShape(11.dp)))
    }
}

/** Fixed widths so the skeleton looks like real, uneven text. */
val SkeletonWidths = listOf(0.62f, 0.48f, 0.7f, 0.55f, 0.66f, 0.44f, 0.58f)
