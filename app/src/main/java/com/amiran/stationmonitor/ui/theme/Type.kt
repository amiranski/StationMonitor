package com.amiran.stationmonitor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.amiran.stationmonitor.R

/** Nunito Sans (SIL Open Font License), close to the font of the company's app. Bundled in res/font. */
val NunitoSans = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal),
    Font(R.font.nunito_sans_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_sans_bold, FontWeight.Bold),
    Font(R.font.nunito_sans_extrabold, FontWeight.ExtraBold),
)

private val Baseline = Typography()

private fun TextStyle.nunito() = copy(fontFamily = NunitoSans)

/** Material 3 type scale with the brand font. Comments say where each style is used. */
val AppTypography = Typography(
    displayLarge = Baseline.displayLarge.nunito(),
    displayMedium = Baseline.displayMedium.nunito(),
    displaySmall = Baseline.displaySmall.nunito(),
    // Screen title ("Stations")
    headlineLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = Baseline.headlineMedium.nunito(),
    // Titles of full-screen messages ("Couldn't load stations")
    headlineSmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    // Top app bar title
    titleLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    // Station name in lists and cards
    titleMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    // Device name
    titleSmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = Baseline.bodyLarge.nunito(),
    bodyMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    // Buttons
    labelLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    // Status pills, counters
    labelMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    // Section labels ("DEVICES"), badges
    labelSmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
)
