package com.msa.freedoom.ui.options

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt
import com.msa.freedoom.AppSettings
import com.msa.freedoom.R

/**
 * Lets the player set common engine options (FOV, brightness, crosshair, dynamic
 * lights, vsync, autosaves, volumes) from the launcher instead of UZDoom's in-game
 * touch menus. A master toggle gates whether the launcher forces them at all — when
 * off, the command line is byte-for-byte what it was before (no `+set` injected), so
 * existing users see no change unless they opt in. See [buildEngineCvarArgs].
 */
@Composable
fun EngineOptionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(AppSettings.getBoolOption(context, EngineOptions.KEY_ENABLED, false))
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.engine_options_header),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        stringResource(R.string.engine_options_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        AppSettings.setBoolOption(context, EngineOptions.KEY_ENABLED, it)
                    },
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    var fov by remember {
                        mutableIntStateOf(AppSettings.getIntOption(context, "eo_fov", EngineOptions.DEFAULT.fov))
                    }
                    IntSlider(
                        label = stringResource(R.string.engine_opt_fov),
                        value = fov,
                        range = EngineOptions.FOV_RANGE,
                        valueText = "$fov°",
                        onChange = { fov = it; EngineOptions.saveFov(context, it) },
                    )

                    var gamma by remember {
                        mutableFloatStateOf(AppSettings.getFloatOption(context, "eo_gamma", EngineOptions.DEFAULT.gamma))
                    }
                    FloatSlider(
                        label = stringResource(R.string.engine_opt_brightness),
                        value = gamma,
                        range = EngineOptions.GAMMA_RANGE,
                        onChange = { gamma = it; EngineOptions.saveGamma(context, it) },
                    )

                    var crosshair by remember {
                        mutableIntStateOf(AppSettings.getIntOption(context, "eo_crosshair", EngineOptions.DEFAULT.crosshair))
                    }
                    IntSlider(
                        label = stringResource(R.string.engine_opt_crosshair),
                        value = crosshair,
                        range = EngineOptions.CROSSHAIR_RANGE,
                        valueText = if (crosshair == 0) stringResource(R.string.engine_opt_off) else "$crosshair",
                        onChange = { crosshair = it; EngineOptions.saveCrosshair(context, it) },
                    )

                    var autosave by remember {
                        mutableIntStateOf(AppSettings.getIntOption(context, "eo_autosave", EngineOptions.DEFAULT.autosaveCount))
                    }
                    IntSlider(
                        label = stringResource(R.string.engine_opt_autosaves),
                        value = autosave,
                        range = EngineOptions.AUTOSAVE_RANGE,
                        valueText = if (autosave == 0) stringResource(R.string.engine_opt_off) else "$autosave",
                        onChange = { autosave = it; EngineOptions.saveAutosaveCount(context, it) },
                    )

                    var masterVol by remember {
                        mutableFloatStateOf(AppSettings.getFloatOption(context, "eo_vol_master", EngineOptions.DEFAULT.masterVolume))
                    }
                    VolumeSlider(
                        label = stringResource(R.string.engine_opt_master_volume),
                        value = masterVol,
                        onChange = { masterVol = it; EngineOptions.saveMasterVolume(context, it) },
                    )

                    var musicVol by remember {
                        mutableFloatStateOf(AppSettings.getFloatOption(context, "eo_vol_music", EngineOptions.DEFAULT.musicVolume))
                    }
                    VolumeSlider(
                        label = stringResource(R.string.engine_opt_music_volume),
                        value = musicVol,
                        onChange = { musicVol = it; EngineOptions.saveMusicVolume(context, it) },
                    )

                    var sfxVol by remember {
                        mutableFloatStateOf(AppSettings.getFloatOption(context, "eo_vol_sfx", EngineOptions.DEFAULT.sfxVolume))
                    }
                    VolumeSlider(
                        label = stringResource(R.string.engine_opt_sfx_volume),
                        value = sfxVol,
                        onChange = { sfxVol = it; EngineOptions.saveSfxVolume(context, it) },
                    )

                    var dynLights by remember {
                        mutableStateOf(AppSettings.getBoolOption(context, "eo_dynlights", EngineOptions.DEFAULT.dynamicLights))
                    }
                    ToggleRow(
                        label = stringResource(R.string.engine_opt_dynamic_lights),
                        checked = dynLights,
                        onChange = { dynLights = it; EngineOptions.saveDynamicLights(context, it) },
                    )

                    var vsync by remember {
                        mutableStateOf(AppSettings.getBoolOption(context, "eo_vsync", EngineOptions.DEFAULT.vsync))
                    }
                    ToggleRow(
                        label = stringResource(R.string.engine_opt_vsync),
                        checked = vsync,
                        onChange = { vsync = it; EngineOptions.saveVsync(context, it) },
                    )

                    var fpsHud by remember {
                        mutableStateOf(AppSettings.getBoolOption(context, "eo_fps", EngineOptions.DEFAULT.fpsHud))
                    }
                    ToggleRow(
                        label = stringResource(R.string.engine_opt_fps_hud),
                        checked = fpsHud,
                        onChange = { fpsHud = it; EngineOptions.saveFpsHud(context, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IntSlider(
    label: String,
    value: Int,
    range: IntRange,
    valueText: String,
    onChange: (Int) -> Unit,
) {
    SliderRow(label, valueText) {
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

@Composable
private fun FloatSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    SliderRow(label, String.format(Locale.US, "%.2f", value)) {
        Slider(
            value = value,
            onValueChange = { onChange(it.coerceIn(range.start, range.endInclusive)) },
            valueRange = range,
        )
    }
}

/** Volume slider showing a 0–100% label over a 0.0–1.0 cvar value. */
@Composable
private fun VolumeSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    SliderRow(label, "${(value * 100).roundToInt()}%") {
        Slider(
            value = value,
            onValueChange = { onChange(it.coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
        )
    }
}

@Composable
private fun SliderRow(label: String, valueText: String, slider: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
        slider()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
