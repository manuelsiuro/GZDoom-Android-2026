package com.msa.freedoom.ui.editor.launch

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.msa.freedoom.AppSettings
import com.msa.freedoom.Game
import com.msa.freedoom.Utils
import com.msa.freedoom.ui.editor.generate.GenerateResult
import com.msa.freedoom.ui.editor.model.MapProject
import com.msa.freedoom.ui.editor.model.WadFormat
import com.msa.freedoom.ui.launch.WadEntry
import com.msa.freedoom.ui.launch.buildLaunchArgs
import java.io.File

/**
 * Boots the GZDoom engine on an already-generated WAD ([result]) for [project]'s chosen
 * test map, reusing the exact launch contract of the launch tab
 * ([com.msa.freedoom.ui.launch.LaunchState.launchGame]): the same Intent extras and
 * [buildLaunchArgs] assembly, just parameterized by the project's IWAD, WAD filename and
 * test-map lump.
 */
/**
 * Ensures the given IWAD is present on disk. The editor tab may be used before the launch tab
 * has unpacked the bundled assets, so a missing Freedoom IWAD is unpacked from assets here.
 * Reused by both the test-launch path and the texture browser (so textures load without
 * needing a test-launch first).
 */
suspend fun ensureIwadUnpacked(activity: Activity, iwadFile: String) = withContext(Dispatchers.IO) {
    AppSettings.createDirectories(activity)
    val iwad = File(AppSettings.getQuakeFullDir(), iwadFile)
    if (!iwad.exists() && iwadFile.lowercase().startsWith("freedoom")) {
        Utils.copyFreedoomFilesToSD(activity)
    }
}

suspend fun launchProject(activity: Activity, project: MapProject, result: GenerateResult) {
    val base = AppSettings.getQuakeFullDir()
    ensureIwadUnpacked(activity, project.iwadFile)
    withContext(Dispatchers.IO) {
        // UZDoom 5.0 (__MOBILE__) loads its base data from ./res relative to the
        // game dir; keep this in sync with LaunchState.launchGame().
        Utils.copyAsset(activity, "uzdoom.pk3", "$base/res")
        Utils.copyAsset(activity, "uzdoom_game_support.pk3", "$base/res")
        Utils.copyAsset(activity, "game_widescreen_gfx.pk3", base)
        Utils.copyAsset(activity, "lights.pk3", base)
        Utils.copyAsset(activity, "brightmaps.pk3", base)
        Utils.copyAsset(activity, "gzdoom.sf2", base)
        Utils.copyAsset(activity, "gzdoom.sf2", "$base/soundfonts")
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
        putExtra("game", "com.msa.freedoom")
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
