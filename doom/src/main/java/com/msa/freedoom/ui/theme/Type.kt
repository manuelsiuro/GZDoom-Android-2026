package com.msa.freedoom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

val DoomTypography = Typography()

/** Monospace style for file paths and command-line arguments. */
@Composable
@ReadOnlyComposable
fun monospaceBody(): TextStyle =
    DoomTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
