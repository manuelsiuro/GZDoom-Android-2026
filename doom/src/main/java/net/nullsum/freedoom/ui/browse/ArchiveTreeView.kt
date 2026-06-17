package net.nullsum.freedoom.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.DoomIcons
import net.nullsum.freedoom.ui.theme.monospaceBody

/**
 * Browses the idgames archive file tree (gamers.org mirror) directly by category —
 * an alternative to keyword search. Folders drill down; tapping a file opens the same
 * detail sheet as a search result. State lives in [BrowseState] so it survives tab
 * switches like in-flight downloads do.
 */
@Composable
fun ArchiveTreeView(
    state: BrowseState,
    onImport: (BrowseEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Back pops one archive level while drilled in (matches the mod picker idiom).
    BackHandler(enabled = state.archivePath.size > 1) { state.archivePopDir() }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { state.archivePopDir() },
                enabled = state.archivePath.size > 1,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.up_one_level),
                )
            }
            Text(
                "/" + state.archivePath.joinToString("/"),
                modifier = Modifier.weight(1f),
                style = monospaceBody(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when {
            state.archiveLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.archiveErrorRes != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    stringResource(state.archiveErrorRes!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.archiveEntries, key = { it.name + if (it.isDir) "/" else "" }) { node ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (node.isDir) state.archivePushDir(node.name)
                                else state.selectedEntry = state.archiveEntryFor(node)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (node.isDir) {
                            Icon(
                                DoomIcons.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                        Spacer(Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                node.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!node.isDir) {
                                Text(
                                    listOfNotNull(
                                        node.sizeBytes?.let { formatSize(it) },
                                        node.lastModified,
                                    ).joinToString("  ·  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
