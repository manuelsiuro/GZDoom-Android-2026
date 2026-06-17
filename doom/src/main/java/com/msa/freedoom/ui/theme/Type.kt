package com.msa.freedoom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A Doom-branded type scale: heavy, slightly tightened display/headline/title styles for
// impactful headers, on top of the default Material body/label scale. No custom font file is
// shipped (keeps the APK lean); the weight + letter-spacing carry the character.
private val base = Typography()

val DoomTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
)

/** Monospace style for file paths and command-line arguments. */
@Composable
@ReadOnlyComposable
fun monospaceBody(): TextStyle =
    DoomTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
