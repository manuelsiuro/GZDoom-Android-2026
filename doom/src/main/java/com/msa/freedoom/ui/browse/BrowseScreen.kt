package com.msa.freedoom.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.msa.freedoom.R
import com.msa.freedoom.idgames.IdgamesApi
import com.msa.freedoom.idgames.IdgamesDirClassifier
import com.msa.freedoom.ui.DoomIcons
import com.msa.freedoom.ui.formatFileSize
import com.msa.freedoom.ui.launch.CompatBadge
import com.msa.freedoom.ui.launch.Verdict
import com.msa.freedoom.ui.launch.badgeText

/** The browse tab: search/browse the idgames archive and download WADs. */
@Composable
fun BrowseScreen(state: BrowseState, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { state.initialize() }
    // Resolve a parked idgames:// deep link once the catalogs/API are ready.
    LaunchedEffect(state.initialized, state.pendingDeeplink) {
        if (state.initialized && state.pendingDeeplink != null) state.resolveDeeplink()
    }
    LaunchedEffect(state.mode) {
        if (state.mode == BrowseMode.ARCHIVE) state.ensureArchiveLoaded()
        if (state.mode == BrowseMode.INSTALLED) state.refreshInstalled()
    }
    // Fetch the rich record whenever a detail sheet opens.
    LaunchedEffect(state.selectedEntry) {
        state.selectedEntry?.let { state.loadDetail(it) }
    }

    var importTarget by remember { mutableStateOf<BrowseEntry?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        importTarget?.let { entry -> if (uri != null) state.importIwad(uri, entry) }
        importTarget = null
    }
    val onImport: (BrowseEntry) -> Unit = { entry ->
        importTarget = entry
        importLauncher.launch(arrayOf("*/*"))
    }

    Column(modifier) {
        ModeToggle(state, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        when (state.mode) {
            BrowseMode.SEARCH -> SearchList(state, onImport, Modifier.weight(1f))
            BrowseMode.ARCHIVE -> ArchiveTreeView(state, onImport, Modifier.weight(1f))
            BrowseMode.INSTALLED -> InstalledView(state, Modifier.weight(1f))
        }
    }

    state.selectedEntry?.let { entry ->
        BrowseDetailSheet(
            entry = entry,
            detail = state.detailState,
            status = state.downloads[entry.downloadKey],
            isInstalled = state.isInstalled(entry),
            onDownload = { state.startDownload(entry) },
            onImport = { onImport(entry) },
            onCancel = { state.cancelDownload(entry.downloadKey) },
            onDelete = { state.pendingDelete = entry },
            onRetryDetail = { state.retryDetail() },
            onDismiss = { state.clearDetail() },
        )
    }

    state.pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { state.pendingDelete = null },
            title = { Text(stringResource(R.string.browse_delete_confirm_title)) },
            text = { Text(stringResource(R.string.browse_delete_confirm_msg, entry.title)) },
            confirmButton = {
                TextButton(onClick = { state.delete(entry) }) {
                    Text(stringResource(R.string.browse_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.pendingDelete = null }) {
                    Text(stringResource(R.string.browse_cancel_download))
                }
            },
        )
    }

    state.pendingBulkDelete?.let { keys ->
        AlertDialog(
            onDismissRequest = { state.pendingBulkDelete = null },
            title = { Text(stringResource(R.string.browse_bulk_delete_title, keys.size)) },
            text = { Text(stringResource(R.string.browse_bulk_delete_msg)) },
            confirmButton = {
                TextButton(onClick = { state.deleteInstalled(keys) }) {
                    Text(stringResource(R.string.browse_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.pendingBulkDelete = null }) {
                    Text(stringResource(R.string.browse_cancel_download))
                }
            },
        )
    }
}

@Composable
private fun ModeToggle(state: BrowseState, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        val modes = listOf(
            BrowseMode.SEARCH to R.string.browse_mode_search,
            BrowseMode.ARCHIVE to R.string.browse_mode_archive,
            BrowseMode.INSTALLED to R.string.browse_mode_installed,
        )
        modes.forEachIndexed { index, (m, labelRes) ->
            SegmentedButton(
                selected = state.mode == m,
                onClick = { state.mode = m },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
            ) {
                Text(stringResource(labelRes))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchList(
    state: BrowseState,
    onImport: (BrowseEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.isSearching,
        onRefresh = { state.retry() },
        modifier = modifier,
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "search") { SearchControls(state) }

        if (state.errorRes != null) {
            item(key = "error") { ErrorBanner(state) }
        }

        if (!state.showingSearch && state.featured.isNotEmpty()) {
            item(key = "featured-header") { SectionHeader(stringResource(R.string.browse_featured_header)) }
            items(state.featured, key = { "featured-${it.filename}" }) { entry ->
                BrowseRow(state, entry.toBrowseEntry(), onImport)
            }
        }

        if (!state.showingSearch && state.classics.isNotEmpty()) {
            item(key = "classic-header") { SectionHeader(stringResource(R.string.browse_classic_header)) }
            items(state.classics, key = { "classic-${it.filename}" }) { entry ->
                BrowseRow(state, entry.toBrowseEntry(), onImport)
            }
        }

        if (!state.showingSearch && state.commercial.isNotEmpty()) {
            item(key = "commercial-header") {
                SectionHeader(stringResource(R.string.browse_commercial_header))
            }
            item(key = "commercial-disclaimer") {
                Text(
                    stringResource(R.string.browse_commercial_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.commercial, key = { "commercial-${it.filename}" }) { entry ->
                BrowseRow(state, entry.toBrowseEntry(), onImport)
            }
        }

        item(key = "results-header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(
                    stringResource(
                        if (state.showingSearch) R.string.browse_results_header
                        else R.string.browse_latest_header,
                    ),
                )
                if (state.isSearching) {
                    Spacer(Modifier.size(12.dp))
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (state.results.isEmpty() && !state.isSearching && state.errorRes == null) {
            item(key = "no-results") {
                Text(
                    stringResource(R.string.browse_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.results, key = { "result-${it.id}" }) { file ->
            BrowseRow(state, file.toBrowseEntry(), onImport)
        }

        item(key = "footer") {
            Text(
                stringResource(R.string.browse_powered_by),
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }
}

@Composable
private fun SearchControls(state: BrowseState) {
    Column(Modifier.padding(top = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { state.query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.browse_search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { state.search() }),
            trailingIcon = {
                IconButton(onClick = { state.search() }) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.browse_search_button))
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val types = listOf(
                IdgamesApi.SearchType.TITLE to R.string.browse_type_title,
                IdgamesApi.SearchType.FILENAME to R.string.browse_type_filename,
                IdgamesApi.SearchType.AUTHOR to R.string.browse_type_author,
            )
            types.forEachIndexed { index, (type, labelRes) ->
                SegmentedButton(
                    selected = state.searchType == type,
                    onClick = { state.searchType = type },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                ) {
                    Text(stringResource(labelRes))
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(state: BrowseState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.errorRes?.let { res ->
                    val detail = state.errorDetail
                    if (detail != null) stringResource(res, detail) else stringResource(res)
                }.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = { state.retry() }) {
                Text(stringResource(R.string.browse_retry))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun BrowseRow(state: BrowseState, entry: BrowseEntry, onImport: (BrowseEntry) -> Unit) {
    val status = state.downloads[entry.downloadKey]
    Card(
        onClick = { state.selectedEntry = entry },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeadingIcon(entry)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        entry.author?.takeIf { it.isNotBlank() }
                            ?.let { stringResource(R.string.browse_by_author, it) },
                        formatFileSize(entry.size).takeIf { entry.size > 0 },
                        entry.rating?.let { rating ->
                            stringResource(R.string.browse_rating, rating, entry.votes ?: 0)
                        },
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Descriptive (no IWAD selected here), so a neutral "likely" pre-download badge.
                if (!entry.isIwad) {
                    val badge = remember(entry.dir, entry.filename) {
                        badgeText(IdgamesDirClassifier.classify(entry.dir, entry.filename))
                    }
                    if (badge.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        CompatBadge(text = badge, verdict = Verdict.UNKNOWN, likely = true)
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            RowAction(state, entry, status, onImport)
        }
    }
}

@Composable
private fun LeadingIcon(entry: BrowseEntry) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (entry.isIwad) DoomIcons.Gamepad else DoomIcons.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RowAction(
    state: BrowseState,
    entry: BrowseEntry,
    status: DownloadStatus?,
    onImport: (BrowseEntry) -> Unit,
) {
    when {
        state.isInstalled(entry) || status is DownloadStatus.Installed -> Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.browse_installed),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
            IconButton(onClick = { state.pendingDelete = entry }) {
                Icon(
                    DoomIcons.Delete,
                    contentDescription = stringResource(R.string.browse_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        status is DownloadStatus.Downloading -> Row(verticalAlignment = Alignment.CenterVertically) {
            DownloadProgressBar(status, Modifier.size(width = 72.dp, height = 6.dp))
            TextButton(onClick = { state.cancelDownload(entry.downloadKey) }) {
                Text(stringResource(R.string.browse_cancel_download))
            }
        }
        status is DownloadStatus.Unzipping -> Text(
            stringResource(R.string.browse_unzipping),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.importOnly -> OutlinedButton(onClick = { onImport(entry) }) {
            Icon(DoomIcons.Download, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.browse_import))
        }
        else -> Button(onClick = { state.startDownload(entry) }) {
            Icon(DoomIcons.Download, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.browse_download))
        }
    }
}

@Composable
fun DownloadProgressBar(status: DownloadStatus.Downloading, modifier: Modifier = Modifier) {
    if (status.total > 0) {
        LinearProgressIndicator(
            progress = { (status.bytes.toFloat() / status.total).coerceIn(0f, 1f) },
            modifier = modifier,
        )
    } else {
        LinearProgressIndicator(modifier = modifier)
    }
}
