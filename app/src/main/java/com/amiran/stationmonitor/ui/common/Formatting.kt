package com.amiran.stationmonitor.ui.common

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.DataError
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "17:28" for today, "24. 11. 2026 17:28" (localized) for older dates.
 * The API sends UTC; it is shown in the phone's time zone.
 */
@Composable
fun formatUpdatedAt(instant: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val dateTime = instant.atZone(zone)
    val formatter = if (dateTime.toLocalDate() == LocalDate.now(zone)) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    }
    return formatter.withLocale(locale).format(dateTime)
}

/**
 * "5 min. ago" / "před 5 min." — localized by Android itself (DateUtils).
 * Used for device lastSeen, which the backend keeps close to "now".
 */
@Composable
fun formatRelativeTime(instant: Instant, now: Instant): String {
    if (now.toEpochMilli() - instant.toEpochMilli() < DateUtils.MINUTE_IN_MILLIS) {
        return stringResource(R.string.just_now)
    }
    return DateUtils.getRelativeTimeSpanString(
        instant.toEpochMilli(),
        now.toEpochMilli(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

/** Current time that ticks every [tickMillis], so "5 min ago" texts stay correct while the screen is open. */
@Composable
fun rememberNow(tickMillis: Long = 30_000): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(tickMillis) {
        while (true) {
            delay(tickMillis)
            now = Instant.now()
        }
    }
    return now
}

/** User-facing text for a [DataError]. */
@Composable
fun DataError.message(): String = when (this) {
    DataError.NoConnection -> stringResource(R.string.error_no_connection)
    DataError.Timeout -> stringResource(R.string.error_timeout)
    is DataError.Server -> stringResource(R.string.error_server, httpCode)
    DataError.Unknown -> stringResource(R.string.error_unknown)
}
