package com.msa.freedoom.ui.options

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.msa.freedoom.AppSettings
import com.msa.freedoom.R
import com.msa.freedoom.ui.backup.BackupCard
import com.msa.freedoom.ui.theme.ThemeController
import com.msa.freedoom.ui.theme.ThemeMode
import com.msa.freedoom.ui.theme.monospaceBody

/** Result of validating a candidate base directory (ports OptionsFragment.updateBaseDir). */
private fun validateBaseDir(context: Context, dir: String): String? {
    val fdir = File(dir)
    if (!fdir.isDirectory) return context.getString(R.string.error_not_directory, dir)
    if (!fdir.canWrite()) return context.getString(R.string.error_not_writable, dir)

    // canWrite() can lie on some SD cards — verify with a real test write.
    val testWrite = File(dir, "test_write")
    try {
        testWrite.createNewFile()
        if (!testWrite.exists()) return context.getString(R.string.error_not_writable, dir)
    } catch (e: IOException) {
        return context.getString(R.string.error_not_writable, dir)
    }
    testWrite.delete()

    if (dir.contains(" ")) return context.getString(R.string.error_contains_spaces, dir)
    return null
}

@Composable
fun OptionsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var basePath by remember { mutableStateOf(AppSettings.freedoomBaseDir.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDirPicker by remember { mutableStateOf(false) }
    var sdCardPathPendingConfirm by remember { mutableStateOf<String?>(null) }

    fun updateBaseDir(dir: String) {
        // validateBaseDir test-writes to the candidate dir and createDirectories does mkdirs;
        // both touch the filesystem, so keep them off the main thread (ANR on slow SD cards).
        scope.launch {
            val error = withContext(Dispatchers.IO) { validateBaseDir(context, dir) }
            if (error != null) {
                errorMessage = error
                return@launch
            }
            AppSettings.freedoomBaseDir = dir
            AppSettings.setStringOption(context, "base_path", dir)
            withContext(Dispatchers.IO) { AppSettings.createDirectories(context) }
            basePath = dir
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.folder_name_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.wad_dir_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(basePath, style = monospaceBody())
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDirPicker = true }) {
                        Text(stringResource(R.string.choose_button_text))
                    }
                    OutlinedButton(onClick = {
                        AppSettings.resetBaseDir(context)
                        updateBaseDir(AppSettings.freedoomBaseDir.orEmpty())
                    }) {
                        Text(stringResource(R.string.reset_button_text))
                    }
                    OutlinedButton(onClick = {
                        val files = context.getExternalFilesDirs(null)
                        if (files.size < 2 || files[1] == null) {
                            errorMessage = context.getString(R.string.sdcard_not_found)
                        } else {
                            sdCardPathPendingConfirm = files[1].toString()
                        }
                    }) {
                        Text(stringResource(R.string.sdcard_button_text))
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.performance_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(12.dp))
                ResolutionDividerDropdown()
            }
        }

        EngineOptionsCard()

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.appearance_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(12.dp))
                ThemeModeDropdown()
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.addons_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(12.dp))
                GoreModToggle()
                Spacer(Modifier.height(12.dp))
                HapticsToggle()
            }
        }

        BackupCard()
    }

    if (showDirPicker) {
        DirectoryPickerDialog(
            initialDir = basePath,
            onDismiss = { showDirPicker = false },
            onChosen = { dir ->
                showDirPicker = false
                updateBaseDir(dir)
            },
        )
    }

    sdCardPathPendingConfirm?.let { path ->
        AlertDialog(
            onDismissRequest = { sdCardPathPendingConfirm = null },
            title = { Text(stringResource(R.string.sdcard_warning_title)) },
            text = { Text(stringResource(R.string.sdcard_warning_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        sdCardPathPendingConfirm = null
                        updateBaseDir(path)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.ok_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { sdCardPathPendingConfirm = null }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.ok_confirm))
                }
            },
        )
    }
}

/**
 * Toggles the bundled Droplets blood/particles add-on (extras_gore.pk3). When on,
 * LaunchState appends `-file extras_gore.pk3` to the engine command line; the PK3
 * carries its own in-game options menu and CVAR defaults.
 */
@Composable
private fun GoreModToggle() {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(AppSettings.getBoolOption(context, "enable_gore_mod", false))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.enable_gore_mod_prompt),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.enable_gore_mod_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                AppSettings.setBoolOption(context, "enable_gore_mod", it)
            },
        )
    }
}

/**
 * Toggles haptic feedback. When on, the engine vibrates the device on weapon fire
 * and when the player is hit (see the native COMMAND_VIBRATE hooks). Backed by the
 * shared "vibrate" pref, read on demand by SDLOpenTouch.CommandHandler in the
 * :Game process.
 */
@Composable
private fun HapticsToggle() {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(AppSettings.getBoolOption(context, "vibrate", true))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.haptics_prompt),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.haptics_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                AppSettings.setBoolOption(context, "vibrate", it)
            },
        )
    }
}

/**
 * Theme picker. Writes through [ThemeController] so the whole app re-themes live.
 * Material You (dynamic colour) is only offered on Android 12+, where it's available.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeDropdown() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val dynamicSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val modes = remember(dynamicSupported) {
        ThemeMode.entries.filter { it != ThemeMode.DYNAMIC || dynamicSupported }
    }

    fun label(mode: ThemeMode): String = context.getString(
        when (mode) {
            ThemeMode.DARK -> R.string.theme_mode_dark
            ThemeMode.LIGHT -> R.string.theme_mode_light
            ThemeMode.SYSTEM -> R.string.theme_mode_system
            ThemeMode.DYNAMIC -> R.string.theme_mode_dynamic
        }
    )

    Text(stringResource(R.string.theme_mode_prompt), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label(ThemeController.mode),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(label(mode)) },
                    onClick = {
                        expanded = false
                        ThemeController.set(context, mode)
                    },
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionDividerDropdown() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var resDiv by remember { mutableStateOf(AppSettings.getIntOption(context, "gzdoom_res_div", 1)) }

    Text(
        stringResource(R.string.divide_resolution_by_prompt),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = resDiv.toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..8).forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.toString()) },
                    onClick = {
                        expanded = false
                        resDiv = value
                        AppSettings.setIntOption(context, "gzdoom_res_div", value)
                    },
                )
            }
        }
    }
}
