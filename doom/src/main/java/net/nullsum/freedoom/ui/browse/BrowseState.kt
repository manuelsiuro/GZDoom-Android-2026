package net.nullsum.freedoom.ui.browse

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
import net.nullsum.freedoom.AppSettings
import net.nullsum.freedoom.R
import net.nullsum.freedoom.idgames.ClassicCatalog
import net.nullsum.freedoom.idgames.ClassicEntry
import net.nullsum.freedoom.idgames.CommercialCatalog
import net.nullsum.freedoom.idgames.CommercialEntry
import net.nullsum.freedoom.idgames.FeaturedCatalog
import net.nullsum.freedoom.idgames.FeaturedEntry
import net.nullsum.freedoom.idgames.IdgamesApi
import net.nullsum.freedoom.idgames.IdgamesFile
import net.nullsum.freedoom.idgames.IdgamesResult
import net.nullsum.freedoom.idgames.WadDownloader

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
)

fun FeaturedEntry.toBrowseEntry() = BrowseEntry(
    title = title,
    author = author,
    description = description,
    dir = dir,
    filename = filename,
    size = size,
    note = note,
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

    private val jobs = mutableMapOf<String, Job>()
    private var searchJob: Job? = null

    val baseDir: String get() = AppSettings.getQuakeFullDir()
    val wadsDir: File get() = File(baseDir, "wads")

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
            } finally {
                isSearching = false
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
                    downloader.downloadAndInstall(entry.dir, entry.filename, wadsDir, onProgress)
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
                    part.renameTo(dest)
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

    /** Deletes an installed entry: a wads/<name>/ folder, or a classic IWAD file. */
    fun delete(entry: BrowseEntry) {
        pendingDelete = null
        scope.launch {
            withContext(Dispatchers.IO) {
                if (entry.isIwad) {
                    File(baseDir, entry.filename).delete()
                } else {
                    File(wadsDir, entry.installName).deleteRecursively()
                }
            }
            downloads.remove(entry.downloadKey)
            refreshInstalled()
        }
    }

    suspend fun refreshInstalled() {
        withContext(Dispatchers.IO) {
            installed = wadsDir.listFiles().orEmpty()
                .filter { it.isDirectory }
                .map { it.name }
                .toSet()
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
