package com.msa.freedoom.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.msa.freedoom.AppSettings

/** How the launcher picks its colour scheme. Persisted via [ThemeController]. */
enum class ThemeMode {
    /** Doom-branded dark (the historical default and brand identity). */
    DARK,

    /** Light counterpart of the Doom palette. */
    LIGHT,

    /** Follow the OS light/dark setting. */
    SYSTEM,

    /** Material You — wallpaper-derived colours (Android 12+; falls back to DARK). */
    DYNAMIC,
}

/**
 * Process-level holder for the active theme mode so a change in Settings re-themes the
 * whole app live, without threading a parameter through every screen. Initialised from
 * prefs in [init]; the Settings UI calls [set] which both persists and updates state.
 */
object ThemeController {
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.DARK)
        private set

    fun init(ctx: Context) {
        val name = AppSettings.getStringOption(ctx, KEY, ThemeMode.DARK.name)
        mode = runCatching { ThemeMode.valueOf(name!!) }.getOrDefault(ThemeMode.DARK)
    }

    fun set(ctx: Context, value: ThemeMode) {
        mode = value
        AppSettings.setStringOption(ctx, KEY, value.name)
    }
}

private val DoomDarkColorScheme = darkColorScheme(
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

private val DoomLightColorScheme = lightColorScheme(
    primary = BloodRedLight,
    onPrimary = OnBloodRedLight,
    primaryContainer = BloodRedContainerLight,
    onPrimaryContainer = OnBloodRedContainerLight,
    secondary = SteelGreyLight,
    onSecondary = OnSteelGreyLight,
    secondaryContainer = SteelGreyContainerLight,
    onSecondaryContainer = OnSteelGreyContainerLight,
    tertiary = BoneAmberLight,
    onTertiary = OnBoneAmberLight,
    tertiaryContainer = BoneAmberContainerLight,
    onTertiaryContainer = OnBoneAmberContainerLight,
    background = SurfaceWhite,
    onBackground = OnSurfaceDark,
    surface = SurfaceWhite,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineGreyLight,
    error = ErrorRedLight,
    onError = OnErrorRedLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

@Composable
fun DoomTheme(
    mode: ThemeMode = ThemeController.mode,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when (mode) {
        ThemeMode.DARK -> DoomDarkColorScheme
        ThemeMode.LIGHT -> DoomLightColorScheme
        ThemeMode.SYSTEM -> if (systemDark) DoomDarkColorScheme else DoomLightColorScheme
        ThemeMode.DYNAMIC -> when {
            !dynamicSupported -> DoomDarkColorScheme
            systemDark -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DoomTypography,
        shapes = DoomShapes,
        content = content,
    )
}
