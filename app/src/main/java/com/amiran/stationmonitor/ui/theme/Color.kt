package com.amiran.stationmonitor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette, taken from the company's own app: navy for text and buttons,
// a red accent, a grey-blue background and white cards.
internal val BrandNavy = Color(0xFF1F2D4A)
internal val BrandRed = Color(0xFFE2402F)
internal val NeutralGround = Color(0xFFF0F1F5)
internal val NeutralTint = Color(0xFFE8EBF2)
internal val NeutralMuted = Color(0xFF5B6478)
internal val NeutralDivider = Color(0xFFDCDFE6)
internal val NeutralOutline = Color(0xFF8A93A6)

// Dark theme counterparts.
internal val DarkGround = Color(0xFF0F1626)
internal val DarkSurface = Color(0xFF1A2337)
internal val DarkTint = Color(0xFF232D44)
internal val DarkOnSurface = Color(0xFFE6E9F0)
internal val DarkMuted = Color(0xFFA9B1C3)
internal val DarkDivider = Color(0xFF2C3650)
internal val DarkPrimary = Color(0xFFAFC3E8)

/** Colors of one device status: pill background, text and the small dot. */
@Immutable
data class StatusColor(
    val container: Color,
    val content: Color,
    val dot: Color,
)

/**
 * Material 3 has no slots for "online / offline / error / unknown", so the app defines its own
 * set and provides it through a CompositionLocal (see Theme.kt).
 * Status is never shown by color alone: the pill always has a text label too.
 */
@Immutable
data class StatusColors(
    val online: StatusColor,
    val offline: StatusColor,
    val error: StatusColor,
    val unknown: StatusColor,
)

internal val LightStatusColors = StatusColors(
    online = StatusColor(container = Color(0xFFE3F4EA), content = Color(0xFF17733F), dot = Color(0xFF22A35A)),
    offline = StatusColor(container = Color(0xFFECEEF2), content = Color(0xFF4A5468), dot = Color(0xFF8A93A6)),
    error = StatusColor(container = Color(0xFFFCE6E3), content = Color(0xFFB42318), dot = BrandRed),
    unknown = StatusColor(container = Color(0xFFFFF1D6), content = Color(0xFF8A5300), dot = Color(0xFFD99A00)),
)

internal val DarkStatusColors = StatusColors(
    online = StatusColor(container = Color(0xFF12351F), content = Color(0xFF7FD9A0), dot = Color(0xFF3FBF6E)),
    offline = StatusColor(container = Color(0xFF2A3142), content = Color(0xFFB9C0CF), dot = Color(0xFF8A93A6)),
    error = StatusColor(container = Color(0xFF3D1A17), content = Color(0xFFFFB4A8), dot = BrandRed),
    unknown = StatusColor(container = Color(0xFF3A2C0C), content = Color(0xFFF2C14E), dot = Color(0xFFD99A00)),
)
