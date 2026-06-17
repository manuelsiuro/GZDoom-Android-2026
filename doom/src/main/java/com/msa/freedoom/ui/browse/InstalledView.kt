package com.msa.freedoom.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.freedoom.R
import com.msa.freedoom.ui.DoomIcons
import com.msa.freedoom.ui.formatFileSize

/**
 * Lists every installed WAD (add-ons under wads/ plus root IWADs) with multi-select
 * and bulk delete (selected or all). State lives in [BrowseState] so a delete in flight
 * and the current selection survive tab switches.
 */
@Composable
fun InstalledView(state: BrowseState, modifier: Modifier = Modifier) {
    val items = state.installedItems
    val selection = state.installedSelection

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.selected_count, selection.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    if (selection.size == items.size) state.clearInstalledSelection()
                    else state.selectAllInstalled()
                },
                enabled = items.isNotEmpty(),
            ) {
                Text(
                    stringResource(
                        if (selection.size == items.size && items.isNotEmpty()) {
                            R.string.clear_button
                        } else {
                            R.string.browse_select_all
                        },
                    ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { state.pendingBulkDelete = selection },
                enabled = selection.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    stringResource(R.string.browse_delete_selected, selection.size),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(
                onClick = { state.pendingBulkDelete = items.map { it.key }.toSet() },
                enabled = items.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.browse_delete_all), color = MaterialTheme.colorScheme.error)
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    stringResource(R.string.browse_installed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.key }) { item ->
                    InstalledRow(
                        item = item,
                        checked = item.key in selection,
                        onToggle = { state.toggleInstalled(item.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledRow(item: InstalledItem, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (item.isIwad) DoomIcons.Gamepad else DoomIcons.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    item.game,
                    formatFileSize(item.size).takeIf { item.size > 0 },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}
