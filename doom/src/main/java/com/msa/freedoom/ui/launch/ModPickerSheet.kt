package com.msa.freedoom.ui.launch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.msa.freedoom.AppSettings
import com.msa.freedoom.R
import com.msa.freedoom.ui.DoomIcons
import com.msa.freedoom.ui.theme.monospaceBody

private data class FsEntry(
    val name: String,
    val isDir: Boolean,
    val relPath: String,
    val verdict: Verdict,
    val badge: String,
)

// Same add-on extensions as the legacy ModSelectDialog, plus .bex (it was accepted by
// getResult() but never listed — a legacy bug).
private val MOD_EXTENSIONS = listOf(".wad", ".pk3", ".pk7", ".deh", ".bex")

/**
 * Bottom-sheet add-on picker replacing ModSelectDialog. Browses the wads/ and mods/
 * subdirectories of the game-data dir; files and whole folders are selected with checkboxes.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ModPickerSheet(
    baseDir: String,
    initialSelection: List<ModEntry>,
    selectedGame: WadEntry?,
    onDismiss: () -> Unit,
    onConfirm: (List<ModEntry>) -> Unit,
) {
    val context = LocalContext.current
    val iwad = remember(selectedGame) { selectedGame?.let { iwadProfile(it.file) } }
    var currentDir by remember { mutableStateOf("wads") }
    var entries by remember { mutableStateOf<List<FsEntry>>(emptyList()) }
    var onlyCompatible by remember { mutableStateOf(false) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(AppSettings.getFavoriteMods(context)) }
    val selected = remember { mutableStateOf(initialSelection) }

    LaunchedEffect(currentDir, iwad) {
        entries = withContext(Dispatchers.IO) { listDir(baseDir, currentDir, iwad) }
    }

    // Files are hidden when they fail an active filter (cross-game INCOMPATIBLE, or non-favorite).
    // Folders ignore the compatibility filter; under the favorites filter a folder stays if it is
    // itself favorited or contains a favorite below it, so nested favorites remain reachable.
    val visible = entries.filter { entry ->
        if (entry.isDir) {
            !onlyFavorites || entry.relPath in favorites ||
                favorites.any { it.startsWith(entry.relPath + "/") }
        } else {
            (!onlyCompatible || entry.verdict != Verdict.INCOMPATIBLE) &&
                (!onlyFavorites || entry.relPath in favorites)
        }
    }

    fun toggle(relPath: String) {
        selected.value = if (selected.value.any { it.relPath == relPath }) {
            selected.value.filterNot { it.relPath == relPath }
        } else {
            selected.value + ModEntry(relPath)
        }
    }

    fun toggleFavorite(relPath: String) {
        favorites = if (relPath in favorites) favorites - relPath else favorites + relPath
        AppSettings.setFavoriteMods(context, favorites)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // Back pops one directory level while browsing a subfolder (legacy KEYCODE_BACK behavior).
        BackHandler(enabled = currentDir.contains('/')) {
            currentDir = currentDir.substringBeforeLast('/')
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            val gameName = selectedGame?.file?.substringBeforeLast('.')
            Text(
                if (gameName != null) {
                    stringResource(R.string.mod_picker_title_for, gameName)
                } else {
                    stringResource(R.string.mod_picker_title)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("wads" to R.string.mod_picker_wads, "mods" to R.string.mod_picker_mods)
                    .forEachIndexed { index, (root, labelRes) ->
                        SegmentedButton(
                            selected = currentDir.substringBefore('/') == root,
                            onClick = { currentDir = root },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { currentDir = currentDir.substringBeforeLast('/') },
                    enabled = currentDir.contains('/'),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.up_one_level),
                    )
                }
                Text("/$currentDir", Modifier.weight(1f), style = monospaceBody())
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onlyFavorites = !onlyFavorites },
                    label = { Text(stringResource(R.string.only_favorites_filter)) },
                    leadingIcon = {
                        Icon(
                            if (onlyFavorites) DoomIcons.Star else DoomIcons.StarOutline,
                            contentDescription = null,
                            Modifier.size(18.dp),
                        )
                    },
                )
                // Compatibility filtering needs a selected game to compare against.
                if (iwad != null) {
                    FilterChip(
                        selected = onlyCompatible,
                        onClick = { onlyCompatible = !onlyCompatible },
                        label = { Text(stringResource(R.string.only_compatible_filter)) },
                    )
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.mod_picker_empty, "$baseDir/$currentDir"),
                            modifier = Modifier.padding(vertical = 24.dp),
                            style = monospaceBody(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(visible, key = { it.relPath }) { entry ->
                    val isChecked = selected.value.any { it.relPath == entry.relPath }
                    val dim = entry.verdict == Verdict.INCOMPATIBLE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (entry.isDir) currentDir = entry.relPath else toggle(entry.relPath)
                            }
                            .padding(vertical = 4.dp)
                            .alpha(if (dim) 0.55f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (entry.isDir) DoomIcons.Folder else DoomIcons.Extension,
                            contentDescription = null,
                            tint = if (entry.isDir) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                            if (entry.badge.isNotEmpty()) {
                                Text(
                                    entry.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = verdictColor(entry.verdict),
                                )
                            }
                        }
                        val fav = entry.relPath in favorites
                        IconButton(onClick = { toggleFavorite(entry.relPath) }) {
                            Icon(
                                if (fav) DoomIcons.Star else DoomIcons.StarOutline,
                                contentDescription = stringResource(
                                    if (fav) R.string.favorite_remove else R.string.favorite_add,
                                ),
                                tint = if (fav) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { toggle(entry.relPath) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.selected_count, selected.value.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { selected.value = emptyList() }) {
                    Text(stringResource(R.string.clear_button))
                }
                Spacer(Modifier.size(8.dp))
                Button(onClick = {
                    onConfirm(selected.value)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.done_button))
                }
            }
        }
    }
}

private fun listDir(baseDir: String, dir: String, iwad: IwadProfile?): List<FsEntry> =
    File("$baseDir/$dir").listFiles().orEmpty()
        .mapNotNull { f ->
            val isDir = f.isDirectory
            // Hide the downloader's hidden staging dir.
            if (isDir && f.name == ".staging") return@mapNotNull null
            if (!isDir && MOD_EXTENSIONS.none { f.name.lowercase().endsWith(it) }) return@mapNotNull null
            val info = detectAddon(f)
            val v = iwad?.let { verdict(it, info) } ?: Verdict.UNKNOWN
            FsEntry(f.name, isDir, "$dir/${f.name}", v, badgeText(info))
        }
        // Folders first (navigation), then most-compatible first, then by name.
        .sortedWith(
            compareByDescending<FsEntry> { it.isDir }
                .thenBy { verdictRank(it.verdict) }
                .thenBy { it.name.lowercase() },
        )
