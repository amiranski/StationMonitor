package com.amiran.stationmonitor.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amiran.stationmonitor.R

/** Visual weight of the action button: filled for errors (do this now), outlined for empty states. */
enum class MessageActionStyle { Primary, Secondary }

/**
 * Centered icon + title + text + button. Used for the error-without-cache and empty states.
 * While [isActionLoading] the button shows a spinner and is disabled.
 */
@Composable
fun FullScreenMessage(
    @DrawableRes icon: Int,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionStyle: MessageActionStyle = MessageActionStyle.Primary,
    isActionLoading: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(40.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        val content: @Composable () -> Unit = {
            if (isActionLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            } else {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text(actionLabel)
        }
        when (actionStyle) {
            MessageActionStyle.Primary -> Button(
                onClick = onAction,
                enabled = !isActionLoading,
                modifier = Modifier.height(48.dp),
            ) { content() }

            MessageActionStyle.Secondary -> OutlinedButton(
                onClick = onAction,
                enabled = !isActionLoading,
                modifier = Modifier.height(48.dp),
            ) { content() }
        }
    }
}
