package com.msa.freedoom.ui.controls

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlin.math.roundToInt
import com.beloko.touchcontrols.TouchSettings
import com.msa.freedoom.R

/**
 * Compose front-end for the on-screen touch-control settings, replacing the legacy
 * AlertDialog/SeekBar UI from [com.beloko.touchcontrols.TouchControlsSettings]. It
 * reads/writes the very same `TouchSettings` prefs (keys mirror TouchControlsSettings'
 * load/save), so the engine picks the values up on its next launch via its own
 * `loadSettings`. Live in-game push (`sendToQuake`) is owned by the game process, where
 * the ControlInterface exists — the launcher only persists.
 */
@Composable
fun TouchControlsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var alpha by remember { mutableIntStateOf(TouchSettings.getIntOption(context, "alpha", 50)) }
    var fwd by remember { mutableIntStateOf(TouchSettings.getIntOption(context, "fwdSens", 50)) }
    var strafe by remember { mutableIntStateOf(TouchSettings.getIntOption(context, "strafeSens", 50)) }
    var pitch by remember { mutableIntStateOf(TouchSettings.getIntOption(context, "pitchSens", 50)) }
    var yaw by remember { mutableIntStateOf(TouchSettings.getIntOption(context, "yawSens", 50)) }

    var mouseMode by remember { mutableStateOf(TouchSettings.getBoolOption(context, "mouse_mode", true)) }
    var invertLook by remember { mutableStateOf(TouchSettings.getBoolOption(context, "invert_look", false)) }
    var precision by remember { mutableStateOf(TouchSettings.getBoolOption(context, "precision_shoot", false)) }
    var showSticks by remember { mutableStateOf(TouchSettings.getBoolOption(context, "show_sticks", false)) }
    var weaponWheel by remember { mutableStateOf(TouchSettings.getBoolOption(context, "enable_ww", true)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.touch_controls_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(stringResource(R.string.touch_sensitivity_header))
                PercentSlider(stringResource(R.string.touch_opacity), alpha) {
                    alpha = it; TouchSettings.setIntOption(context, "alpha", it)
                }
                SensSlider(stringResource(R.string.touch_forward_sens), fwd) {
                    fwd = it; TouchSettings.setIntOption(context, "fwdSens", it)
                }
                SensSlider(stringResource(R.string.touch_strafe_sens), strafe) {
                    strafe = it; TouchSettings.setIntOption(context, "strafeSens", it)
                }
                SensSlider(stringResource(R.string.touch_pitch_sens), pitch) {
                    pitch = it; TouchSettings.setIntOption(context, "pitchSens", it)
                }
                SensSlider(stringResource(R.string.touch_yaw_sens), yaw) {
                    yaw = it; TouchSettings.setIntOption(context, "yawSens", it)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(stringResource(R.string.touch_behaviour_header))
                ToggleRow(stringResource(R.string.touch_mouse_mode), mouseMode) {
                    mouseMode = it; TouchSettings.setBoolOption(context, "mouse_mode", it)
                }
                ToggleRow(stringResource(R.string.touch_invert_look), invertLook) {
                    invertLook = it; TouchSettings.setBoolOption(context, "invert_look", it)
                }
                ToggleRow(stringResource(R.string.touch_precision_shoot), precision) {
                    precision = it; TouchSettings.setBoolOption(context, "precision_shoot", it)
                }
                ToggleRow(stringResource(R.string.touch_show_sticks), showSticks) {
                    showSticks = it; TouchSettings.setBoolOption(context, "show_sticks", it)
                }
                ToggleRow(stringResource(R.string.touch_weapon_wheel), weaponWheel) {
                    weaponWheel = it; TouchSettings.setBoolOption(context, "enable_ww", it)
                }
            }
        }

        OutlinedButton(onClick = {
            applyDefaults(context)
            // Re-read into local state.
            alpha = 50; fwd = 50; strafe = 50; pitch = 50; yaw = 50
            mouseMode = true; invertLook = false; precision = false; showSticks = false; weaponWheel = true
        }) {
            Text(stringResource(R.string.touch_reset_defaults))
        }
    }
}

private fun applyDefaults(ctx: Context) {
    TouchSettings.setIntOption(ctx, "alpha", 50)
    TouchSettings.setIntOption(ctx, "fwdSens", 50)
    TouchSettings.setIntOption(ctx, "strafeSens", 50)
    TouchSettings.setIntOption(ctx, "pitchSens", 50)
    TouchSettings.setIntOption(ctx, "yawSens", 50)
    TouchSettings.setBoolOption(ctx, "mouse_mode", true)
    TouchSettings.setBoolOption(ctx, "invert_look", false)
    TouchSettings.setBoolOption(ctx, "precision_shoot", false)
    TouchSettings.setBoolOption(ctx, "show_sticks", false)
    TouchSettings.setBoolOption(ctx, "enable_ww", true)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.tertiary,
    )
    Spacer(Modifier.height(8.dp))
}

/** 0–100 % opacity. */
@Composable
private fun PercentSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    LabeledSlider(label, "$value%", value.toFloat(), 0f..100f) { onChange(it.roundToInt()) }
}

/** Sensitivity 0–100 → the engine divides by 50 (so 50 = 1.0×); shown as a multiplier. */
@Composable
private fun SensSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    val multiplier = String.format(java.util.Locale.US, "%.1f×", value / 50f)
    LabeledSlider(label, multiplier, value.toFloat(), 0f..100f) { onChange(it.roundToInt()) }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
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
