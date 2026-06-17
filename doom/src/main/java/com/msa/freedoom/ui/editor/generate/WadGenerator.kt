package com.msa.freedoom.ui.editor.generate

import android.content.Context
import com.doomandroid.png2wad.Png2WadConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.msa.freedoom.AppSettings
import com.msa.freedoom.ui.editor.data.sanitizeWadName
import com.msa.freedoom.ui.editor.model.MapProject
import java.io.File

/** Result of a successful generation: the written WAD and how many maps it holds. */
data class GenerateResult(val wadFile: File, val mapCount: Int)

/**
 * Renders every map in [project] to a PNG, writes the tuned Preferences.ini, and runs the
 * native png2wad converter to produce `<base>/mods/<sanitized-name>.wad`. Each PNG becomes
 * MAP01, MAP02, … in array order (or ExMy for the Doom1 format). The output is a nodeless
 * PWAD; GZDoom builds the nodes on load. Returns null on failure.
 *
 * [onProgress] is invoked as `(mapsRendered, totalMaps)` during the render phase so the UI
 * can show "rendering k/N" before the single (potentially multi-second) native call.
 */
suspend fun generateWad(
    context: Context,
    project: MapProject,
    onProgress: (Int, Int) -> Unit = { _, _ -> },
): GenerateResult? = withContext(Dispatchers.IO) {
    runCatching {
        val genDir = File(context.cacheDir, "editor_gen").apply {
            deleteRecursively()
            mkdirs()
        }
        val total = project.maps.size
        val pngPaths = project.maps.mapIndexed { i, map ->
            val png = File(genDir, "map_%02d.png".format(i + 1))
            renderMapPng(map, png)
            onProgress(i + 1, total)
            png.absolutePath
        }.toTypedArray()

        val ini = File(genDir, "Preferences.ini").apply { writeText(buildPreferencesIni(project)) }

        val modsDir = File(AppSettings.getQuakeFullDir(), "mods").apply { mkdirs() }
        val wadFile = File(modsDir, sanitizeWadName(project.name) + ".wad")

        // Per-map thing descriptors (only the valid, in-bounds, open-floor ones).
        val perMapThings = project.maps.map { encodeThings(validThings(it)) }
        val hasPlacedThings = perMapThings.any { it.isNotEmpty() }

        val converter = Png2WadConverter()
        val ok = if (hasPlacedThings || project.manualThings) {
            converter.generateWadWithThings(
                pngPaths,
                perMapThings.toTypedArray(),
                project.manualThings,
                wadFile.absolutePath,
                ini.absolutePath,
            )
        } else {
            converter.generateWad(pngPaths, wadFile.absolutePath, ini.absolutePath)
        }
        if (ok && wadFile.exists()) GenerateResult(wadFile, total) else null
    }.getOrElse {
        it.printStackTrace()
        null
    }
}
