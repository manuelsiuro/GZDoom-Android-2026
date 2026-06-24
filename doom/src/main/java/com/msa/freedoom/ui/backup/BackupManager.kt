package com.msa.freedoom.ui.backup

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.msa.freedoom.AppSettings

/**
 * Backs up and restores the player's progress (savegames) and all launcher settings
 * (the OPTIONS prefs — which also hold mod profiles, engine options, theme, stats) to a
 * single `.zip` at a user-chosen SAF location. Self-contained (no network, no account);
 * the Drive-sync variant is intentionally out of scope.
 */
object BackupManager {

    private const val SAVES_PREFIX = "saves/"
    private const val PREFS_ENTRY = "prefs.json"
    const val DEFAULT_FILENAME = "freedoom-backup.zip"

    private fun savesDir(): File = File(AppSettings.getQuakeFullDir(), "gzdoom_saves")

    /** Writes a backup zip to [out]. Returns the number of save files included. */
    fun backup(ctx: Context, out: Uri): Int {
        var saveCount = 0
        val resolver = ctx.contentResolver
        resolver.openOutputStream(out)?.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                val saves = savesDir()
                if (saves.isDirectory) {
                    saves.walkTopDown().filter { it.isFile }.forEach { file ->
                        val rel = file.relativeTo(saves).path.replace(File.separatorChar, '/')
                        zip.putNextEntry(ZipEntry(SAVES_PREFIX + rel))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        saveCount++
                    }
                }
                zip.putNextEntry(ZipEntry(PREFS_ENTRY))
                zip.write(prefsToJson(ctx).toString().toByteArray())
                zip.closeEntry()
            }
        } ?: throw java.io.IOException("Cannot open backup destination")
        return saveCount
    }

    /** Restores savegames and settings from the backup zip at [input]. */
    fun restore(ctx: Context, input: Uri) {
        val saves = savesDir().apply { mkdirs() }
        val destRoot = saves.canonicalPath + File.separator
        val resolver = ctx.contentResolver
        resolver.openInputStream(input)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> {}
                        entry.name == PREFS_ENTRY -> applyPrefs(ctx, zip.readBytes().decodeToString())
                        entry.name.startsWith(SAVES_PREFIX) -> {
                            val outFile = File(saves, entry.name.removePrefix(SAVES_PREFIX))
                            if (!outFile.canonicalPath.startsWith(destRoot)) {
                                throw SecurityException("Backup entry escapes saves dir: ${entry.name}")
                            }
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        } ?: throw java.io.IOException("Cannot open backup file")
        AppSettings.reloadSettings(ctx)
    }

    // --- prefs (de)serialization ------------------------------------------

    private fun prefsToJson(ctx: Context): JSONObject {
        val prefs = ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE)
        val root = JSONObject()
        for ((key, value) in prefs.all) {
            val typed = JSONObject()
            when (value) {
                is Boolean -> typed.put("t", "b").put("v", value)
                is Int -> typed.put("t", "i").put("v", value)
                is Long -> typed.put("t", "l").put("v", value)
                is Float -> typed.put("t", "f").put("v", value.toDouble())
                is String -> typed.put("t", "s").put("v", value)
                is Set<*> -> typed.put("t", "set").put("v", org.json.JSONArray(value.map { it.toString() }))
                else -> continue
            }
            root.put(key, typed)
        }
        return root
    }

    private fun applyPrefs(ctx: Context, jsonText: String) {
        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return
        val editor = ctx.getSharedPreferences("OPTIONS", Context.MODE_PRIVATE).edit()
        for (key in root.keys()) {
            val typed = root.optJSONObject(key) ?: continue
            when (typed.optString("t")) {
                "b" -> editor.putBoolean(key, typed.optBoolean("v"))
                "i" -> editor.putInt(key, typed.optInt("v"))
                "l" -> editor.putLong(key, typed.optLong("v"))
                "f" -> editor.putFloat(key, typed.optDouble("v").toFloat())
                "s" -> editor.putString(key, typed.optString("v"))
                "set" -> {
                    val arr = typed.optJSONArray("v") ?: continue
                    editor.putStringSet(key, (0 until arr.length()).map { arr.optString(it) }.toSet())
                }
            }
        }
        editor.apply()
    }
}
