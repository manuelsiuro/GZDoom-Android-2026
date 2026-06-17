package com.msa.freedoom.idgames

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.msa.freedoom.ui.launch.GameFamily
import com.msa.freedoom.ui.launch.detectAddon
import com.msa.freedoom.ui.launch.gameFolderName
import okhttp3.Request

/**
 * Downloads an idgames zip from the first working mirror, unpacks it to a staging
 * dir, detects which game it targets, and moves it into `wadsDir/<game>/<installName>/`
 * (e.g. `wads/doom2/scythe/`). All blocking work runs on [Dispatchers.IO]; the `.part`
 * temp file and the staging/install dirs are always cleaned up on failure or cancellation.
 */
class WadDownloader(private val client: okhttp3.OkHttpClient = IdgamesApi.sharedClient) {

    sealed interface Result {
        data class Installed(
            val gameFolder: String,
            val installName: String,
            val contentFiles: List<String>,
        ) : Result
        data class Failed(val reason: FailReason) : Result
    }

    enum class FailReason { ALL_MIRRORS_FAILED, NO_USABLE_FILES }

    suspend fun downloadAndInstall(
        dir: String,
        filename: String,
        wadsDir: File,
        gameGuess: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val installName = filename.substringBeforeLast('.')
        val part = File(wadsDir, "$installName.zip.part")
        val staging = File(wadsDir, ".staging/$installName")
        wadsDir.mkdirs()
        var installDir: File? = null

        try {
            staging.deleteRecursively()
            if (!downloadToFile(dir, filename, part, onProgress)) {
                return@withContext Result.Failed(FailReason.ALL_MIRRORS_FAILED)
            }
            val contentFiles = try {
                ZipInstaller.install(part, staging)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }
            if (contentFiles.isEmpty()) {
                staging.deleteRecursively()
                return@withContext Result.Failed(FailReason.NO_USABLE_FILES)
            }
            val gameFolder = routeFolder(staging, contentFiles, gameGuess)
            installDir = File(wadsDir, "$gameFolder/$installName")
            placeInstall(staging, installDir)
            Result.Installed(gameFolder, installName, contentFiles)
        } catch (e: CancellationException) {
            staging.deleteRecursively()
            installDir?.deleteRecursively()
            throw e
        } finally {
            part.delete()
            if (staging.exists()) staging.deleteRecursively()
        }
    }

    /**
     * Downloads a plain .wad from [url] straight to [destFile] (used for the classic-games
     * catalog — no zip, no mirrors). Streams to a `.part` file, renamed on success.
     */
    suspend fun downloadIwad(
        url: String,
        destFile: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val part = File(destFile.path + ".part")
        destFile.parentFile?.mkdirs()
        try {
            streamToFile(url, part, onProgress)
            if (!part.renameTo(destFile)) throw IOException("rename failed")
            // IWADs live at the game-dir root, not under a wads/<game>/ folder.
            Result.Installed("", destFile.nameWithoutExtension, listOf(destFile.name))
        } catch (e: IOException) {
            Result.Failed(FailReason.ALL_MIRRORS_FAILED)
        } finally {
            part.delete()
        }
    }

    /**
     * Confirms the target game by lump-scanning the freshly-extracted content (the
     * definitive signal), falling back to the pre-download [gameGuess] when detection
     * is inconclusive. The primary file is the first .wad/.pk3; otherwise the dir itself.
     */
    internal fun routeFolder(staging: File, contentFiles: List<String>, gameGuess: String): String {
        val primary = contentFiles
            .firstOrNull { it.lowercase().let { n -> n.endsWith(".wad") || n.endsWith(".pk3") } }
            ?.let { File(staging, it) } ?: staging
        val detected = detectAddon(primary)
        return if (detected.family == GameFamily.UNKNOWN) gameGuess else gameFolderName(detected)
    }

    /** Moves [staging] to [target], idempotently replacing any existing install. */
    internal fun placeInstall(staging: File, target: File) {
        target.parentFile?.mkdirs()
        target.deleteRecursively()
        if (staging.renameTo(target)) return
        staging.copyRecursively(target, overwrite = true)
        staging.deleteRecursively()
    }

    /** Tries each mirror in order; returns false if all fail. */
    private suspend fun downloadToFile(
        dir: String,
        filename: String,
        dest: File,
        onProgress: (Long, Long) -> Unit,
    ): Boolean {
        for (mirror in IdgamesMirrors.MIRRORS) {
            try {
                streamToFile(IdgamesMirrors.downloadUrl(mirror, dir, filename), dest, onProgress)
                return true
            } catch (e: IOException) {
                dest.delete()
            }
        }
        return false
    }

    private suspend fun streamToFile(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        val call = client.newCall(Request.Builder().url(url).build())
        // execute() on Dispatchers.IO; cancellation is observed per chunk below and
        // the in-flight request is torn down when the coroutine completes abnormally.
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty body")
                val total = body.contentLength()
                var read = 0L
                onProgress(0L, total)
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            kotlin.coroutines.coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            onProgress(read, total)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            call.cancel()
            throw e
        }
    }
}
