package net.nullsum.freedoom.ui.browse

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.nullsum.freedoom.R
import net.nullsum.freedoom.idgames.IdgamesDirClassifier
import net.nullsum.freedoom.idgames.IdgamesFile
import net.nullsum.freedoom.idgames.TextfileParser
import net.nullsum.freedoom.ui.DoomIcons
import net.nullsum.freedoom.ui.launch.AddonInfo
import net.nullsum.freedoom.ui.launch.CompatBadge
import net.nullsum.freedoom.ui.launch.GameFamily
import net.nullsum.freedoom.ui.launch.Verdict
import net.nullsum.freedoom.ui.launch.badgeText
import net.nullsum.freedoom.ui.theme.monospaceBody

/** Bottom-sheet "WAD info" page: rich metadata, text file, reviews, and the download action. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BrowseDetailSheet(
    entry: BrowseEntry,
    detail: DetailState?,
    status: DownloadStatus?,
    isInstalled: Boolean,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRetryDetail: () -> Unit,
    onDismiss: () -> Unit,
) {
    val loaded = (detail as? DetailState.Loaded)?.file

    // Layered compat: a confident family from the text file when available, else a dir guess.
    val guess = remember(entry.dir, entry.filename) {
        IdgamesDirClassifier.classify(entry.dir, entry.filename)
    }
    val (compat, likely) = remember(loaded) {
        val fromTxt = loaded?.textfile
            ?.let { TextfileParser.parse(it) }
            ?.takeIf { it.family != GameFamily.UNKNOWN }
            ?.let { AddonInfo(it.family, it.slot) }
        if (fromTxt != null) fromTxt to false else guess to true
    }

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
                    (loaded?.author ?: entry.author)?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(R.string.browse_by_author, it) },
                    loaded?.date ?: entry.date,
                    formatSize(entry.size).takeIf { entry.size > 0 },
                    (loaded?.rating?.takeIf { loaded.votes > 0 } ?: entry.rating)?.let {
                        stringResource(R.string.browse_rating, it, loaded?.votes ?: entry.votes ?: 0)
                    },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val badge = badgeText(compat)
            if (badge.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                CompatBadge(text = badge, verdict = Verdict.UNKNOWN, likely = likely)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                if (entry.isIwad) entry.filename else entry.dir + entry.filename,
                style = monospaceBody(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (badge.isNotEmpty() && !entry.isIwad) {
                Spacer(Modifier.height(8.dp))
                PlayWithCard(badge)
            }

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
                    (loaded?.description ?: entry.description).orEmpty()
                        .ifBlank { stringResource(R.string.browse_no_results) },
                    style = MaterialTheme.typography.bodyMedium,
                )

                when (detail) {
                    DetailState.Loading -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    is DetailState.Error -> Column(Modifier.padding(top = 12.dp)) {
                        Text(
                            stringResource(detail.res),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetryDetail) {
                            Text(stringResource(R.string.browse_retry))
                        }
                    }
                    is DetailState.Loaded -> RichDetail(detail.file)
                    null -> {}
                }
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
private fun PlayWithCard(badge: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Text(
            stringResource(R.string.browse_play_with, badge),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun RichDetail(file: IdgamesFile) {
    InfoField(stringResource(R.string.browse_field_base), file.base)
    InfoField(stringResource(R.string.browse_field_credits), file.credits)
    InfoField(stringResource(R.string.browse_field_buildtime), file.buildtime)
    InfoField(stringResource(R.string.browse_field_editors), file.editors)
    InfoField(stringResource(R.string.browse_field_bugs), file.bugs, error = true)

    file.textfile?.takeIf { it.isNotBlank() }?.let { TextfileSection(it) }

    val reviews = file.reviewList.filter { !it.text.isNullOrBlank() }
    if (reviews.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.browse_reviews_header),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        reviews.forEach { review ->
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    "★".repeat(review.vote.coerceIn(0, 5)).takeIf { it.isNotEmpty() },
                    review.username?.takeIf { it.isNotBlank() },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(review.text.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoField(label: String, value: String?, error: Boolean = false) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(8.dp))
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        value.trim(),
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun TextfileSection(textfile: String) {
    var expanded by remember { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.browse_textfile),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
        )
    }
    if (expanded) {
        Text(textfile.trim(), style = monospaceBody())
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
