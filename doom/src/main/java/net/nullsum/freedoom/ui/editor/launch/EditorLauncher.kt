package net.nullsum.freedoom.ui.editor.launch

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nullsum.freedoom.AppSettings
import net.nullsum.freedoom.Game
import net.nullsum.freedoom.Utils
import net.nullsum.freedoom.ui.editor.generate.GenerateResult
import net.nullsum.freedoom.ui.editor.model.MapProject
import net.nullsum.freedoom.ui.editor.model.WadFormat
import net.nullsum.freedoom.ui.launch.WadEntry
import net.nullsum.freedoom.ui.launch.buildLaunchArgs
import java.io.File

/**
 * Boots the GZDoom engine on an already-generated WAD ([result]) for [project]'s chosen
 * test map, reusing the exact launch contract of the launch tab
 * ([net.nullsum.freedoom.ui.launch.LaunchState.launchGame]): the same Intent extras and
 * [buildLaunchArgs] assembly, just parameterized by the project's IWAD, WAD filename and
 * test-map lump.
 */
suspend fun launchProject(activity: Activity, project: MapProject, result: GenerateResult) {
    val base = AppSettings.getQuakeFullDir()
    withContext(Dispatchers.IO) {
        AppSettings.createDirectories(activity)
        // The editor tab may be used before the launch tab has unpacked the bundled assets.
        // If the chosen IWAD is a Freedoom one and missing, unpack; engine resources always.
        val iwad = File(base, project.iwadFile)
        if (!iwad.exists() && project.iwadFile.lowercase().startsWith("freedoom")) {
            Utils.copyFreedoomFilesToSD(activity)
        }
        Utils.copyAsset(activity, "gzdoom.pk3", base)
        Utils.copyAsset(activity, "gzdoom.sf2", base)
    }

    val iwadArgs = WadEntry(project.iwadFile, 0L).iwadArgs
    val modArgs = "-file mods/${result.wadFile.name} "
    // ZDoom/GZDoom -skill is 1-based (1 = ITYTD … 5 = Nightmare).
    val extraArgs = "${testMapLump(project)} -skill ${project.skill.coerceIn(1, 5)}"

    val intent = Intent(activity, Game::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory(Intent.CATEGORY_LAUNCHER)
        putExtra("res_div", AppSettings.getIntOption(activity, "gzdoom_res_div", 1))
        putExtra("game_path", base)
        putExtra("game", "net.nullsum.freedoom")
        putExtra("args", buildLaunchArgs(iwadArgs, modArgs, extraArgs, base))
    }
    activity.startActivity(intent)
}

/** "+map MAP0n" for Doom2, "+map E{episode}M{n}" for Doom1 (map number clamped to 1..9). */
private fun testMapLump(project: MapProject): String {
    val slot = project.testMapIndex.coerceIn(0, project.maps.lastIndex)
    return if (project.format == WadFormat.DOOM1) {
        "+map E${project.episode.coerceIn(1, 9)}M${(slot + 1).coerceIn(1, 9)}"
    } else {
        "+map MAP%02d".format(slot + 1)
    }
}
