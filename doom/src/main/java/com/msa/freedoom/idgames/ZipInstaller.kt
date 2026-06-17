package com.msa.freedoom.idgames

import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

/**
 * Unpacks a downloaded idgames zip into an install directory, keeping only files
 * the engine (or the user) can use. Lives outside the legacy [com.msa.freedoom.Utils]
 * grab-bag so it stays pure-Kotlin and JVM-testable.
 */
object ZipInstaller {

    /** Playable content — matches the add-on extensions ModPickerSheet lists. */
    val CONTENT_EXTENSIONS = setOf("wad", "pk3", "pk7", "deh", "bex")

    /** Extracted too (readmes), but doesn't count as a successful install on its own. */
    val EXTRA_EXTENSIONS = setOf("txt")

    /**
     * Extracts the allowed entries of [zip] into [destDir] (created if needed),
     * preserving the archive's internal relative paths.
     *
     * @return relative paths of the extracted *content* files; empty means the
     *   archive held nothing playable and the caller should delete [destDir].
     * @throws SecurityException if an entry would escape [destDir] (zip-slip).
     */
    fun install(zip: File, destDir: File): List<String> {
        destDir.mkdirs()
        val destRoot = destDir.canonicalPath + File.separator
        val contentFiles = mutableListOf<String>()

        ZipInputStream(FileInputStream(zip).buffered()).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                if (entry.isDirectory) continue
                val extension = entry.name.substringAfterLast('.', "").lowercase()
                if (extension !in CONTENT_EXTENSIONS && extension !in EXTRA_EXTENSIONS) continue

                val outFile = File(destDir, entry.name)
                if (!outFile.canonicalPath.startsWith(destRoot)) {
                    throw SecurityException("Zip entry escapes install dir: ${entry.name}")
                }
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { stream.copyTo(it) }
                if (extension in CONTENT_EXTENSIONS) contentFiles.add(entry.name)
            }
        }
        return contentFiles
    }
}
