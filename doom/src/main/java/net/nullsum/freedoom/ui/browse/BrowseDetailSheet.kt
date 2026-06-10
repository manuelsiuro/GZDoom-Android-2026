package net.nullsum.freedoom.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.DoomIcons
import net.nullsum.freedoom.ui.theme.monospaceBody

/** Bottom-sheet detail view for one archive entry, with the download action. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BrowseDetailSheet(
    entry: BrowseEntry,
    status: DownloadStatus?,
    isInstalled: Boolean,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(entry.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    entry.author?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(R.string.browse_by_author, it) },
                    entry.date,
                    formatSize(entry.size).takeIf { entry.size > 0 },
                    entry.rating?.let { stringResource(R.string.browse_rating, it, entry.votes ?: 0) },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (entry.isIwad) entry.filename else entry.dir + entry.filename,
                style = monospaceBody(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (entry.note.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    entry.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    entry.description.orEmpty().ifBlank { stringResource(R.string.browse_no_results) },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(16.dp))
            ActionArea(
                entry = entry,
                status = status,
                isInstalled = isInstalled,
                onDownload = onDownload,
                onImport = onImport,
                onCancel = onCancel,
                onDelete = onDelete,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionArea(
    entry: BrowseEntry,
    status: DownloadStatus?,
    isInstalled: Boolean,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    when {
        isInstalled || status is DownloadStatus.Installed -> Column {
            Text(
                stringResource(R.string.browse_installed) + " — " +
                    when {
                        entry.importOnly -> stringResource(R.string.browse_imported_hint)
                        entry.isIwad -> stringResource(R.string.browse_installed_iwad_hint)
                        else -> stringResource(R.string.browse_installed_hint, entry.installName)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDelete) {
                Icon(
                    DoomIcons.Delete,
                    contentDescription = null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.browse_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        status is DownloadStatus.Downloading -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DownloadProgressBar(status, Modifier.weight(1f).height(6.dp))
            if (status.total > 0) {
                Text(
                    "${(status.bytes * 100 / status.total).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.browse_cancel_download))
            }
        }
        status is DownloadStatus.Unzipping -> Text(
            stringResource(R.string.browse_unzipping),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            if (status is DownloadStatus.Failed) {
                Text(
                    stringResource(status.reasonRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (entry.importOnly) {
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(DoomIcons.Download, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.browse_import))
                }
            } else {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Icon(DoomIcons.Download, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.browse_download))
                }
            }
        }
    }
}
