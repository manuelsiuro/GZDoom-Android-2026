package net.nullsum.freedoom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DoomColorScheme = darkColorScheme(
    primary = BloodRed,
    onPrimary = OnBloodRed,
    primaryContainer = BloodRedContainer,
    onPrimaryContainer = OnBloodRedContainer,
    secondary = SteelGrey,
    onSecondary = OnSteelGrey,
    secondaryContainer = SteelGreyContainer,
    onSecondaryContainer = OnSteelGreyContainer,
    tertiary = BoneAmber,
    onTertiary = OnBoneAmber,
    tertiaryContainer = BoneAmberContainer,
    onTertiaryContainer = OnBoneAmberContainer,
    background = SurfaceBlack,
    onBackground = OnSurfaceLight,
    surface = SurfaceBlack,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineGrey,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

@Composable
fun DoomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DoomColorScheme,
        typography = DoomTypography,
        content = content,
    )
}
