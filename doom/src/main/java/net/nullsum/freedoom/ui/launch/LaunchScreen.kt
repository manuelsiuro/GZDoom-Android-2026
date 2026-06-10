package net.nullsum.freedoom.ui.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.launch
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.DoomIcons
import net.nullsum.freedoom.ui.theme.monospaceBody

@Composable
fun LaunchScreen(
    state: LaunchState,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var showModPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { state.initialize() }

    if (state.isPreparing) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.preparing_game_data))
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        GameListPane(
            state = state,
            onRefresh = { scope.launch { state.refreshGames() } },
            onOpenOptions = onOpenOptions,
            modifier = Modifier.weight(0.55f),
        )
        LaunchPane(
            state = state,
            onAddMods = { showModPicker = true },
            onLaunch = { scope.launch { state.launchGame() } },
            modifier = Modifier.weight(0.45f),
        )
    }

    if (showModPicker) {
        ModPickerSheet(
            baseDir = state.baseDir,
            initialSelection = state.selectedMods.toList(),
            onDismiss = { showModPicker = false },
            onConfirm = { mods ->
                state.selectedMods.clear()
                state.selectedMods.addAll(mods)
            },
        )
    }
}

@Composable
private fun GameListPane(
    state: LaunchState,
    onRefresh: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.select_game_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { state.clearSelection() },
                enabled = state.selectedGame != null || state.selectedMods.isNotEmpty(),
            ) {
                Text(stringResource(R.string.reset_button_text))
            }
        }
        Spacer(Modifier.height(4.dp))

        if (state.games.isEmpty()) {
            EmptyGamesState(state.baseDir, onRefresh, onOpenOptions)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.games, key = { it.file }) { game ->
                    GameCard(
                        game = game,
                        selected = game == state.selectedGame,
                        onClick = { state.selectGame(game) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: WadEntry, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                DoomIcons.Gamepad,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(game.file, style = MaterialTheme.typography.titleSmall)
                Text(
                    formatFileSize(game.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyGamesState(baseDir: String, onRefresh: () -> Unit, onOpenOptions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            DoomIcons.Folder,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.no_wads_found), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.no_iwads_err) + baseDir,
            style = monospaceBody(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.refresh_button))
            }
            OutlinedButton(onClick = onOpenOptions) {
                Icon(Icons.Default.Settings, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.open_options_button))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LaunchPane(
    state: LaunchState,
    onAddMods: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                state.selectedGame?.file ?: stringResource(R.string.select_game_to_launch),
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.selectedGame != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Text(
                stringResource(R.string.addons_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(8.dp))
            if (state.selectedMods.isEmpty()) {
                Text(
                    stringResource(R.string.no_mods_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.selectedMods.forEach { mod ->
                        InputChip(
                            selected = true,
                            onClick = { state.selectedMods.remove(mod) },
                            label = { Text(mod.name) },
                            leadingIcon = if (mod.isFolder) {
                                { Icon(DoomIcons.Folder, contentDescription = null, Modifier.size(18.dp)) }
                            } else null,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_mod),
                                    Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAddMods) {
                Text(stringResource(R.string.add_mods_button))
            }
            Spacer(Modifier.height(20.dp))

            ExtraArgsField(state)
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onLaunch,
            enabled = state.selectedGame != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.start_full), style = MaterialTheme.typography.titleMedium)
        }
        if (state.selectedGame == null) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.launch_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExtraArgsField(state: LaunchState) {
    var historyExpanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = state.extraArgs,
        onValueChange = { state.extraArgs = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.extra_args_label)) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        singleLine = true,
        trailingIcon = {
            Row {
                if (state.extraArgs.isNotEmpty()) {
                    IconButton(onClick = { state.extraArgs = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_args))
                    }
                }
                if (state.argsHistory.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { historyExpanded = true }) {
                            Icon(DoomIcons.History, contentDescription = stringResource(R.string.args_history))
                        }
                        DropdownMenu(
                            expanded = historyExpanded,
                            onDismissRequest = { historyExpanded = false },
                        ) {
                            state.argsHistory.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(entry, style = monospaceBody()) },
                                    onClick = {
                                        state.extraArgs = entry
                                        historyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1 shl 20 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1 shl 10 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
