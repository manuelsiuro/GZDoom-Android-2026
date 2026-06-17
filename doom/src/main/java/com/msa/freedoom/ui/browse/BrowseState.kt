package com.msa.freedoom.ui.browse

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.msa.freedoom.AppSettings
import com.msa.freedoom.R
import com.msa.freedoom.idgames.ClassicCatalog
import com.msa.freedoom.idgames.ClassicEntry
import com.msa.freedoom.idgames.CommercialCatalog
import com.msa.freedoom.idgames.CommercialEntry
import com.msa.freedoom.idgames.FeaturedCatalog
import com.msa.freedoom.idgames.FeaturedEntry
import com.msa.freedoom.idgames.ArchiveListingParser
import com.msa.freedoom.idgames.IdgamesApi
import com.msa.freedoom.idgames.IdgamesDirClassifier
import com.msa.freedoom.idgames.IdgamesFile
import com.msa.freedoom.idgames.IdgamesResult
import com.msa.freedoom.idgames.IdgamesUri
import com.msa.freedoom.idgames.WadDownloader
import com.msa.freedoom.idgames.ZipInstaller
import com.msa.freedoom.ui.launch.gameFolderName
import com.msa.freedoom.ui.launch.scanIwads

/** One row/sheet target, unifying API results, featured and classic-catalog entries. */
data class BrowseEntry(
    val title: String,
    val author: String?,
    val description: String?,
    val dir: String,
    val filename: String,
    val size: Long,
    val rating: Double? = null,
    val votes: Int? = null,
    val date: String? = null,
    val note: String = "",
    /** Set for classic-games entries: a plain .wad fetched from this URL into the game dir. */
    val directUrl: String? = null,
    val isIwad: Boolean = false,
    /** Set for commercial games: never downloaded — the user imports their own .wad. */
    val importOnly: Boolean = false,
    /** idgames file id, when this entry came from a search/latest result. */
    val id: Long? = null,
    /** Archive file path ("dir/filename") for fetching the full record via `get` by path. */
    val fetchPath: String? = null,
) {
    val installName: String get() = filename.substringBeforeLast('.')

    /** Key into [BrowseState.downloads]: IWADs install as a single file, zips as a dir. */
    val downloadKey: String get() = if (isIwad) filename else installName
}

fun IdgamesFile.toBrowseEntry() = BrowseEntry(
    title = title?.takeIf { it.isNotBlank() } ?: filename,
    author = author,
    description = description,
    dir = dir,
    filename = filename,
    size = size,
    rating = rating.takeIf { votes > 0 },
    votes = votes,
    date = date,
    id = id.takeIf { it > 0 },
    fetchPath = (dir + filename).takeIf { dir.isNotBlank() && filename.isNotBlank() },
)

fun FeaturedEntry.toBrowseEntry() = BrowseEntry(
    title = title,
    author = author,
    description = description,
    dir = dir,
    filename = filename,
    size = size,
    note = note,
    // Featured entries are real idgames megawads — enrich them by path on the detail view.
    fetchPath = (dir + filename).takeIf { dir.isNotBlank() && filename.isNotBlank() },
)

fun ClassicEntry.toBrowseEntry() = BrowseEntry(
    title = title,
    author = null,
    description = description,
    dir = "",
    filename = filename,
    size = size,
    note = note,
    directUrl = downloadUrl,
    isIwad = true,
)

fun CommercialEntry.toBrowseEntry() = BrowseEntry(
    title = title,
    author = null,
    description = description,
    dir = "",
    filename = filename,
    size = 0,
    note = note,
    isIwad = true,
    importOnly = true,
)

sealed interface DownloadStatus {
    data class Downloading(val bytes: Long, val total: Long) : DownloadStatus
    data object Unzipping : DownloadStatus
    data class Failed(val reasonRes: Int) : DownloadStatus
    data object Installed : DownloadStatus
}

/** Which Browse view is showing: keyword search, the archive folder tree, or installed WADs. */
enum class BrowseMode { SEARCH, ARCHIVE, INSTALLED }

/** One installed WAD shown in the Installed view (an add-on under wads/, or a root IWAD). */
data class InstalledItem(
    val key: String,
    val name: String,
    /** Game-folder label for an add-on (e.g. "doom2"); null for a flat install or an IWAD. */
    val game: String?,
    val isIwad: Boolean,
    /** wads/-relative path for an add-on, or the IWAD filename at the game-dir root. */
    val relOrFile: String,
    val size: Long,
)

/** Lazy load state of the rich per-entry detail (textfile, reviews, …) via the `get` action. */
sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val file: IdgamesFile) : DetailState
    data class Error(val res: Int) : DetailState
}

/**
 * State holder for the browse tab. Constructed at MainScreen level with MainScreen's
 * coroutine scope so in-flight downloads survive tab swipes (the pager disposes this
 * tab's composition when the user parks two pages away).
 */
@Stable
class BrowseState(
    private val context: Context,
    private val scope: CoroutineScope,
    private val api: IdgamesApi = IdgamesApi(),
    private val downloader: WadDownloader = WadDownloader(),
) {
    var featured by mutableStateOf(listOf<FeaturedEntry>())
        private set
    var classics by mutableStateOf(listOf<ClassicEntry>())
        private set
    var commercial by mutableStateOf(listOf<CommercialEntry>())
        private set
    var results by mutableStateOf(listOf<IdgamesFile>())
        private set
    var query by mutableStateOf("")
    var searchType by mutableStateOf(IdgamesApi.SearchType.TITLE)
    var isSearching by mutableStateOf(false)
        private set
    var showingSearch by mutableStateOf(false)
        private set
    var errorRes by mutableStateOf<Int?>(null)
        private set
    var errorDetail by mutableStateOf<String?>(null)
        private set
    var selectedEntry by mutableStateOf<BrowseEntry?>(null)
    var pendingDelete by mutableStateOf<BrowseEntry?>(null)
    var initialized by mutableStateOf(false)
        private set

    /** Keyed by [BrowseEntry.downloadKey]. */
    val downloads = mutableStateMapOf<String, DownloadStatus>()
    var installed by mutableStateOf(setOf<String>())
        private set
    var installedIwads by mutableStateOf(setOf<String>())
        private set

    /** Per-game install paths ("doom2/scythe") so a nested install can be located for delete. */
    var installedRel by mutableStateOf(setOf<String>())
        private set

    /** Flat list of everything installed, for the Installed view + bulk delete. */
    var installedItems by mutableStateOf(listOf<InstalledItem>())
        private set
    var installedSelection by mutableStateOf(setOf<String>())
        private set
    var pendingBulkDelete by mutableStateOf<Set<String>?>(null)

    var mode by mutableStateOf(BrowseMode.SEARCH)

    // --- archive tree browser (gamers.org file tree under levels/) ---
    var archivePath by mutableStateOf(listOf("levels"))
        private set
    var archiveEntries by mutableStateOf(listOf<ArchiveListingParser.Node>())
        private set
    var archiveLoading by mutableStateOf(false)
        private set
    var archiveErrorRes by mutableStateOf<Int?>(null)
        private set

    // --- lazy rich detail + deep link ---
    var detailState by mutableStateOf<DetailState?>(null)
        private set
    var pendingDeeplink by mutableStateOf<android.net.Uri?>(null)
        private set

    private val jobs = mutableMapOf<String, Job>()
    private var searchJob: Job? = null
    private var archiveJob: Job? = null
    private var detailJob: Job? = null

    val baseDir: String get() = AppSettings.getQuakeFullDir()
    val wadsDir: File get() = File(baseDir, "wads")

    /** Full idgames path of the current archive directory, with trailing slash. */
    private val archiveDir: String get() = archivePath.joinToString("/") + "/"

    suspend fun initialize() {
        if (initialized) return
        withContext(Dispatchers.IO) {
            featured = FeaturedCatalog.load(context)
            classics = ClassicCatalog.load(context)
            commercial = CommercialCatalog.load(context)
        }
        refreshInstalled()
        initialized = true
        loadLatest()
    }

    fun retry() {
        if (showingSearch) search() else scope.launch { loadLatest() }
    }

    private suspend fun loadLatest() {
        isSearching = true
        clearError()
        try {
            when (val result = api.latest(20)) {
                is IdgamesResult.Success -> {
                    results = result.value
                    showingSearch = false
                }
                is IdgamesResult.ApiError -> setApiError(result)
            }
        } catch (e: IOException) {
            errorRes = R.string.browse_error_offline
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorRes = R.string.browse_error_api
            errorDetail = e.message
        } finally {
            isSearching = false
        }
    }

    fun search() {
        val trimmed = query.trim()
        if (trimmed.length < 3) {
            errorRes = R.string.browse_query_too_short
            errorDetail = null
            return
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            isSearching = true
            clearError()
            try {
                when (val result = api.search(trimmed, searchType)) {
                    is IdgamesResult.Success -> {
                        results = result.value
                        showingSearch = true
                    }
                    is IdgamesResult.ApiError -> setApiError(result)
                }
            } catch (e: IOException) {
                errorRes = R.string.browse_error_offline
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Non-IO failures (e.g. malformed JSON) used to crash the scope.
                errorRes = R.string.browse_error_api
                errorDetail = e.message
            } finally {
                isSearching = false
            }
        }
    }

    // --- archive tree navigation ---

    /** Loads the current archive directory if not already loaded (called on entering the tab). */
    fun ensureArchiveLoaded() {
        if (archiveEntries.isEmpty() && archiveJob?.isActive != true) loadArchive()
    }

    fun archivePushDir(name: String) {
        archivePath = archivePath + name
        loadArchive()
    }

    fun archivePopDir() {
        if (archivePath.size > 1) {
            archivePath = archivePath.dropLast(1)
            loadArchive()
        }
    }

    /** Jumps to a breadcrumb segment (0 = first segment). */
    fun archiveJumpTo(index: Int) {
        if (index in 0 until archivePath.lastIndex) {
            archivePath = archivePath.take(index + 1)
            loadArchive()
        }
    }

    private fun loadArchive() {
        archiveJob?.cancel()
        archiveJob = scope.launch {
            archiveLoading = true
            archiveErrorRes = null
            archiveEntries = emptyList()
            try {
                val nodes = api.listArchive(archivePath.joinToString("/"))
                    // Only folders and downloadable .zip archives — hide .txt/.diz/etc.
                    .filter { it.isDir || it.name.endsWith(".zip", ignoreCase = true) }
                // Folders first, then files, each alphabetical — matches the mod picker idiom.
                archiveEntries = nodes.sortedWith(
                    compareByDescending<ArchiveListingParser.Node> { it.isDir }
                        .thenBy { it.name.lowercase() },
                )
                if (nodes.isEmpty()) archiveErrorRes = R.string.browse_no_results
            } catch (e: IOException) {
                archiveErrorRes = R.string.browse_error_offline
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Directory-index HTML changed shape / failed to parse — don't crash the scope.
                archiveErrorRes = R.string.browse_error_generic
            } finally {
                archiveLoading = false
            }
        }
    }

    /** Builds a downloadable entry from an archive-tree file node in the current directory. */
    fun archiveEntryFor(node: ArchiveListingParser.Node): BrowseEntry = BrowseEntry(
        title = node.name,
        author = null,
        description = null,
        dir = archiveDir,
        filename = node.name,
        size = node.sizeBytes ?: 0,
        date = node.lastModified,
        fetchPath = archiveDir + node.name,
    )

    // --- rich detail ---

    /** Fetches the full record for [entry] (textfile, reviews, …). No-op for catalog-only entries. */
    fun loadDetail(entry: BrowseEntry) {
        val ref = entry.id?.let { IdgamesUri.Ref.ById(it) }
            ?: entry.fetchPath?.let { IdgamesUri.Ref.ByPath(it) }
        detailJob?.cancel()
        if (ref == null) {
            detailState = null
            return
        }
        detailState = DetailState.Loading
        detailJob = scope.launch {
            try {
                detailState = when (val r = api.resolve(ref)) {
                    is IdgamesResult.Success ->
                        r.value?.let { DetailState.Loaded(it) }
                            ?: DetailState.Error(R.string.browse_no_results)
                    is IdgamesResult.ApiError -> DetailState.Error(R.string.browse_error_api)
                }
            } catch (e: IOException) {
                detailState = DetailState.Error(R.string.browse_error_offline)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                detailState = DetailState.Error(R.string.browse_error_generic)
            }
        }
    }

    fun retryDetail() {
        selectedEntry?.let { loadDetail(it) }
    }

    fun clearDetail() {
        detailJob?.cancel()
        detailState = null
        selectedEntry = null
    }

    // --- deep links (idgames://) ---

    fun onDeeplink(uri: android.net.Uri) {
        pendingDeeplink = uri
    }

    /** Resolves a parked idgames:// link and opens its detail sheet. Call once [initialized]. */
    fun resolveDeeplink() {
        val uri = pendingDeeplink ?: return
        pendingDeeplink = null
        val ref = IdgamesUri.parse(uri.toString())
        if (ref == null) {
            errorRes = R.string.browse_deeplink_failed
            errorDetail = uri.toString()
            return
        }
        scope.launch {
            try {
                when (val r = api.resolve(ref)) {
                    is IdgamesResult.Success -> {
                        val file = r.value
                        if (file != null) {
                            // Opening the sheet triggers loadDetail via MainScreen's effect.
                            selectedEntry = file.toBrowseEntry()
                        } else {
                            errorRes = R.string.browse_deeplink_failed
                            errorDetail = uri.toString()
                        }
                    }
                    is IdgamesResult.ApiError -> {
                        errorRes = R.string.browse_deeplink_failed
                        errorDetail = uri.toString()
                    }
                }
            } catch (e: IOException) {
                errorRes = R.string.browse_error_offline
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorRes = R.string.browse_deeplink_failed
                errorDetail = uri.toString()
            }
        }
    }

    fun isInstalled(entry: BrowseEntry): Boolean =
        if (entry.isIwad) entry.filename.lowercase() in installedIwads
        else entry.installName in installed

    fun startDownload(entry: BrowseEntry) {
        val name = entry.downloadKey
        if (name.isEmpty() || isInstalled(entry) || jobs[name]?.isActive == true) return
        downloads[name] = DownloadStatus.Downloading(0, -1)
        jobs[name] = scope.launch {
            try {
                val onProgress = { bytes: Long, total: Long ->
                    if (downloads[name] !is DownloadStatus.Unzipping) {
                        downloads[name] = DownloadStatus.Downloading(bytes, total)
                        if (!entry.isIwad && total in 1..bytes) {
                            downloads[name] = DownloadStatus.Unzipping
                        }
                    }
                }
                val result = if (entry.directUrl != null) {
                    downloader.downloadIwad(entry.directUrl, File(baseDir, entry.filename), onProgress)
                } else {
                    val gameGuess = gameFolderName(
                        IdgamesDirClassifier.classify(entry.dir, entry.filename),
                    )
                    downloader.downloadAndInstall(entry.dir, entry.filename, wadsDir, gameGuess, onProgress)
                }
                when (result) {
                    is WadDownloader.Result.Installed -> {
                        downloads[name] = DownloadStatus.Installed
                        refreshInstalled()
                    }
                    is WadDownloader.Result.Failed -> downloads[name] = DownloadStatus.Failed(
                        when (result.reason) {
                            WadDownloader.FailReason.ALL_MIRRORS_FAILED -> R.string.browse_error_mirrors
                            WadDownloader.FailReason.NO_USABLE_FILES -> R.string.browse_error_no_files
                        },
                    )
                }
            } catch (e: CancellationException) {
                downloads.remove(name)
                throw e
            } catch (e: Exception) {
                // Any other failure (disk full mid-unzip, rename failure, zip-slip rejection)
                // would otherwise leave the row stuck in Downloading and crash the scope.
                downloads[name] = DownloadStatus.Failed(R.string.browse_error_mirrors)
            }
        }
    }

    fun cancelDownload(downloadKey: String) {
        jobs[downloadKey]?.cancel()
    }

    /**
     * Copies a user-picked .wad (from a copy they own) into the game dir under the
     * entry's canonical IWAD name, so it shows up in Select game. Never downloads.
     */
    fun importIwad(uri: android.net.Uri, entry: BrowseEntry) {
        val name = entry.downloadKey
        if (jobs[name]?.isActive == true) return
        downloads[name] = DownloadStatus.Downloading(0, -1)
        jobs[name] = scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val dest = File(baseDir, entry.filename)
                val part = File(dest.path + ".part")
                try {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: return@withContext false
                    input.use { src -> part.outputStream().use { src.copyTo(it) } }
                    part.renameTo(dest).also { renamed -> if (!renamed) part.delete() }
                } catch (e: Exception) {
                    part.delete()
                    false
                }
            }
            if (ok) {
                downloads[name] = DownloadStatus.Installed
                refreshInstalled()
            } else {
                downloads[name] = DownloadStatus.Failed(R.string.browse_import_failed)
            }
        }
    }

    /** Deletes an installed entry: a wads/<game>/<name>/ folder, or a classic IWAD file. */
    fun delete(entry: BrowseEntry) {
        pendingDelete = null
        scope.launch {
            withContext(Dispatchers.IO) {
                if (entry.isIwad) {
                    File(baseDir, entry.filename).delete()
                } else {
                    // Prefer the known nested path; fall back to a legacy flat wads/<name>/.
                    val rel = installedRel.firstOrNull { it.substringAfterLast('/') == entry.installName }
                    val target = if (rel != null) File(wadsDir, rel) else File(wadsDir, entry.installName)
                    target.deleteRecursively()
                }
            }
            downloads.remove(entry.downloadKey)
            refreshInstalled()
        }
    }

    // --- Installed view: selection + bulk delete ---

    fun toggleInstalled(key: String) {
        installedSelection = if (key in installedSelection) {
            installedSelection - key
        } else {
            installedSelection + key
        }
    }

    fun selectAllInstalled() {
        installedSelection = installedItems.map { it.key }.toSet()
    }

    fun clearInstalledSelection() {
        installedSelection = emptySet()
    }

    /** Deletes the given installed items (add-on folders and/or root IWADs), then refreshes. */
    fun deleteInstalled(keys: Set<String>) {
        pendingBulkDelete = null
        if (keys.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                installedItems.filter { it.key in keys }.forEach { item ->
                    if (item.isIwad) {
                        File(baseDir, item.relOrFile).delete()
                    } else {
                        File(wadsDir, item.relOrFile).deleteRecursively()
                        downloads.remove(item.name)
                    }
                }
            }
            installedSelection = emptySet()
            refreshInstalled()
        }
    }

    suspend fun refreshInstalled() {
        withContext(Dispatchers.IO) {
            val names = mutableSetOf<String>()
            val rels = mutableSetOf<String>()
            val items = mutableListOf<InstalledItem>()
            // Layout: wads/<game>/<installName>/. A directory that directly holds content
            // files is a leaf install; otherwise it's a game folder whose children are
            // installs. This keeps legacy flat wads/<installName>/ (incl. ones with their
            // own subfolders) counted as installed for back-compat.
            wadsDir.listFiles().orEmpty()
                .filter { it.isDirectory && it.name != ".staging" }
                .forEach { top ->
                    if (top.holdsContent()) {
                        names += top.name // legacy flat install
                        items += InstalledItem(
                            key = top.name, name = top.name, game = null,
                            isIwad = false, relOrFile = top.name, size = top.dirSize(),
                        )
                    } else {
                        top.listFiles().orEmpty().filter { it.isDirectory }.forEach { child ->
                            names += child.name
                            val rel = "${top.name}/${child.name}"
                            rels += rel
                            items += InstalledItem(
                                key = rel, name = child.name, game = top.name,
                                isIwad = false, relOrFile = rel, size = child.dirSize(),
                            )
                        }
                    }
                }
            // Selectable IWADs at the game-dir root (engine pk3s excluded by scanIwads).
            scanIwads(baseDir).forEach { iwad ->
                items += InstalledItem(
                    key = "iwad:${iwad.file}", name = iwad.file, game = null,
                    isIwad = true, relOrFile = iwad.file, size = iwad.sizeBytes,
                )
            }
            installed = names
            installedRel = rels
            installedItems = items.sortedWith(
                compareByDescending<InstalledItem> { it.isIwad }.thenBy { it.name.lowercase() },
            )
            // Drop selections whose items no longer exist.
            installedSelection = installedSelection.intersect(items.map { it.key }.toSet())
            installedIwads = File(baseDir).listFiles().orEmpty()
                .filter { !it.isDirectory }
                .map { it.name.lowercase() }
                .toSet()
        }
    }

    private fun setApiError(error: IdgamesResult.ApiError) {
        errorRes = R.string.browse_error_api
        errorDetail = error.message
    }

    private fun clearError() {
        errorRes = null
        errorDetail = null
    }
}

/** True if this directory directly contains an engine-usable file (a leaf install dir). */
private fun File.holdsContent(): Boolean = listFiles().orEmpty().any {
    !it.isDirectory && it.extension.lowercase() in ZipInstaller.CONTENT_EXTENSIONS
}

/** Total size of a directory tree, for display in the Installed view. */
private fun File.dirSize(): Long = walkTopDown().filter { it.isFile }.sumOf { it.length() }
