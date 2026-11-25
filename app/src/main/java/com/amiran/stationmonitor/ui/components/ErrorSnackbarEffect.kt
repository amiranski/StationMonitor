package com.amiran.stationmonitor.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.amiran.stationmonitor.R
import com.amiran.stationmonitor.domain.model.DataError
import com.amiran.stationmonitor.ui.common.message

/**
 * Shows "Couldn't refresh. Server error (500)." with a Retry action whenever [error] is set.
 *
 * The error is part of the UI state (not a one-shot event): after the Snackbar was shown the
 * screen calls [onShown], the ViewModel clears the error, and it will not reappear after a
 * rotation. This follows Google's "UI events as state" guidance.
 */
@Composable
fun ErrorSnackbarEffect(
    error: DataError?,
    hostState: SnackbarHostState,
    onRetry: () -> Unit,
    onShown: () -> Unit,
) {
    if (error == null) return
    val message = stringResource(R.string.refresh_failed, error.message())
    val actionLabel = stringResource(R.string.action_retry)
    // rememberUpdatedState: the effect below keeps running, but always calls the latest lambdas.
    val currentOnRetry by rememberUpdatedState(onRetry)
    val currentOnShown by rememberUpdatedState(onShown)

    LaunchedEffect(error) {
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
        )
        currentOnShown()
        if (result == SnackbarResult.ActionPerformed) currentOnRetry()
    }
}
