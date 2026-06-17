package com.msa.freedoom.ui.editor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.msa.freedoom.AppSettings
import com.msa.freedoom.ui.editor.model.MapProject
import java.io.File

/** A saved project on disk, for the projects list UI. */
data class ProjectSummary(val name: String, val file: File, val mapCount: Int, val lastModified: Long)

/**
 * JSON persistence for [MapProject]s under `<getQuakeFullDir()>/editor_projects/`.
 * `_current.json` is the auto-saved working project; `<sanitized-name>.json` are explicit
 * named saves. All reads tolerate corruption (return null / skip) so a bad file never
 * crashes the editor; the `schemaVersion` field plus `ignoreUnknownKeys` leave room to
 * evolve the format.
 */
object ProjectStore {
    private const val CURRENT_FILE = "_current.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private fun dir(): File = File(AppSettings.getQuakeFullDir(), "editor_projects").apply { mkdirs() }

    /** Atomic write via a temp file + rename so a kill mid-write can't truncate the project. */
    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".part")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    suspend fun autosave(project: MapProject) = withContext(Dispatchers.IO) {
        runCatching { writeAtomic(File(dir(), CURRENT_FILE), json.encodeToString(project)) }
        Unit
    }

    suspend fun loadCurrent(): MapProject? = withContext(Dispatchers.IO) {
        val f = File(dir(), CURRENT_FILE)
        if (!f.exists()) return@withContext null
        runCatching { json.decodeFromString<MapProject>(f.readText()) }.getOrNull()
    }

    suspend fun saveNamed(project: MapProject): File = withContext(Dispatchers.IO) {
        val f = File(dir(), sanitizeWadName(project.name) + ".json")
        writeAtomic(f, json.encodeToString(project))
        f
    }

    suspend fun list(): List<ProjectSummary> = withContext(Dispatchers.IO) {
        dir().listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".json") && it.name != CURRENT_FILE }
            .mapNotNull { f ->
                val p = runCatching { json.decodeFromString<MapProject>(f.readText()) }.getOrNull()
                    ?: return@mapNotNull null
                ProjectSummary(p.name, f, p.maps.size, f.lastModified())
            }
            .sortedByDescending { it.lastModified }
    }

    suspend fun load(file: File): MapProject? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<MapProject>(file.readText()) }.getOrNull()
    }

    suspend fun delete(file: File): Boolean = withContext(Dispatchers.IO) { file.delete() }
}

/**
 * Turns a user-facing project name into a safe WAD/JSON base filename: lowercase, only
 * `[a-z0-9_-]`, collapsed repeats, trimmed of leading/trailing separators, capped at 32
 * chars, never empty (falls back to "mymap").
 */
fun sanitizeWadName(raw: String): String {
    val cleaned = raw.lowercase()
        .map { if (it in 'a'..'z' || it in '0'..'9' || it == '-') it else '_' }
        .joinToString("")
        .replace(Regex("_+"), "_")
        .trim('_', '-')
        .take(32)
        .trim('_', '-')
    return cleaned.ifEmpty { "mymap" }
}
